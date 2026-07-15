--
-- PostgreSQL database dump
--

\restrict yqFZgGi6U41h2bOZ9lrnvA4WWunusMrrTwheZzuAIpIhd7XuulHlgLQrcLdYJhx

-- Dumped from database version 17.6
-- Dumped by pg_dump version 18.4

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: public; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA public;


--
-- Name: SCHEMA public; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON SCHEMA public IS 'standard public schema';


--
-- Name: submission_status; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.submission_status AS ENUM (
    'PENDING',
    'APPROVED',
    'REJECTED'
);


--
-- Name: submission_type; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.submission_type AS ENUM (
    'NEW_ALTERNATIVE',
    'NEW_APP',
    'UPDATE'
);


--
-- Name: check_signing_key_threshold(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.check_signing_key_threshold() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
declare
  vote_count int;
  v_category text;
begin
  -- Count how many distinct users submitted the same (package_name, sha256_digest)
  select count(*)
  into vote_count
  from signing_key_votes
  where package_name = NEW.package_name
    and sha256_digest = NEW.sha256_digest;

  if vote_count >= 3 then
    
    -- Check if it was previously classified as a proprietary target and grab its category
    select category into v_category 
    from targets 
    where package_name = NEW.package_name;

    -- If it wasn't in targets or had no category, set a default required category
    if v_category is null then
      v_category := 'System';
    end if;

    -- Remove it from proprietary targets since it's verified FOSS
    delete from targets
    where package_name = NEW.package_name;

    -- Insert it into solutions (FOSS apps)
    -- We add a note in source_repo/description indicating it was auto-approved
    insert into solutions (
      package_name, 
      name, 
      category, 
      source_repo, 
      description
    )
    values (
      NEW.package_name, 
      NEW.app_label, 
      v_category, 
      'Community Verified', 
      'Auto-approved via community signing key verification.'
    )
    on conflict (package_name) do update
      set source_repo = 'Community Verified';
      
  end if;

  return NEW;
end;
$$;


--
-- Name: check_submission_auto_approval(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.check_submission_auto_approval() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    net_votes INT;
    sub_table TEXT;
    sub_record RECORD;
BEGIN
    -- Calculate net votes for this submission
    SELECT 
        COALESCE(SUM(CASE WHEN vote = 1 THEN 1 ELSE 0 END), 0) - COALESCE(SUM(CASE WHEN vote = -1 THEN 1 ELSE 0 END), 0),
        MAX(submission_table)
    INTO net_votes, sub_table
    FROM submission_votes
    WHERE submission_id = NEW.submission_id;

    IF net_votes >= 5 THEN
        IF sub_table = 'user_submissions' THEN
            -- Check if it's already approved
            SELECT * INTO sub_record FROM user_submissions WHERE id = NEW.submission_id;
            
            IF sub_record.status = 'PENDING' THEN
                -- Mark as APPROVED
                UPDATE user_submissions SET status = 'APPROVED' WHERE id = NEW.submission_id;
                
                -- Insert into solutions or targets based on submission_type
                IF sub_record.submission_type = 'SOLUTION' THEN
                    INSERT INTO solutions (
                        package_name, name, description, license, repo_url, fdroid_id, category
                    ) VALUES (
                        sub_record.app_package, 
                        sub_record.app_name, 
                        sub_record.description, 
                        sub_record.license, 
                        sub_record.repo_url, 
                        sub_record.fdroid_id, 
                        sub_record.category
                    )
                    ON CONFLICT (package_name) DO NOTHING;
                    
                ELSIF sub_record.submission_type = 'TARGET' THEN
                    INSERT INTO targets (
                        package_name, name, description, category, alternatives
                    ) VALUES (
                        sub_record.app_package, 
                        sub_record.app_name, 
                        sub_record.description, 
                        sub_record.category,
                        sub_record.alternatives
                    )
                    ON CONFLICT (package_name) DO NOTHING;
                END IF;
            END IF;
            
        ELSIF sub_table = 'user_linking_submissions' THEN
            -- Handle linking submissions if they hit 5 votes
            SELECT * INTO sub_record FROM user_linking_submissions WHERE id = NEW.submission_id;
            
            IF sub_record.status = 'PENDING' THEN
                UPDATE user_linking_submissions SET status = 'APPROVED' WHERE id = NEW.submission_id;
                
                -- Append the new alternatives to the existing targets table
                UPDATE targets 
                SET alternatives = array_cat(
                    COALESCE(alternatives, ARRAY[]::TEXT[]), 
                    sub_record.alternatives
                )
                WHERE package_name = sub_record.proprietary_package;
            END IF;
        END IF;
    END IF;
    
    RETURN NEW;
END;
$$;


--
-- Name: delete_account(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.delete_account() RETURNS void
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
begin
  -- Delete the user's profile
  delete from public.profiles where id = auth.uid();
end;
$$;


--
-- Name: get_alternatives_with_match_votes(text, uuid); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.get_alternatives_with_match_votes(target_pkg text, p_user_id uuid DEFAULT NULL::uuid) RETURNS TABLE(package_name text, name text, license text, repo_url text, fdroid_id text, icon_url text, description text, category text, features text[], pros text[], cons text[], rating_privacy double precision, rating_usability double precision, rating_features double precision, vote_count integer, match_upvotes bigint, match_downvotes bigint, match_score bigint, user_match_vote integer)
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
BEGIN
    RETURN QUERY
    SELECT
        s.package_name, s.name, s.license, s.repo_url,
        s.fdroid_id, s.icon_url, s.description, s.category,
        s.features, s.pros, s.cons,
        s.rating_privacy, s.rating_usability, s.rating_features,
        s.vote_count,
        COUNT(*) FILTER (WHERE tsv.vote = 1)   AS match_upvotes,
        COUNT(*) FILTER (WHERE tsv.vote = -1)  AS match_downvotes,
        COALESCE(SUM(tsv.vote), 0)             AS match_score,
        MAX(CASE WHEN tsv.user_id = p_user_id THEN tsv.vote END)::integer AS user_match_vote
    FROM targets t
    JOIN solutions s ON s.package_name = ANY(
        SELECT unnest(t.alternatives)
    )
    LEFT JOIN target_solution_votes tsv
        ON tsv.target_package = t.package_name
       AND tsv.solution_package = s.package_name
    WHERE t.package_name = target_pkg
    GROUP BY s.package_name, s.name, s.license, s.repo_url,
             s.fdroid_id, s.icon_url, s.description, s.category,
             s.features, s.pros, s.cons,
             s.rating_privacy, s.rating_usability, s.rating_features, s.vote_count;
END;
$$;


--
-- Name: get_global_sovereignty_stats(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.get_global_sovereignty_stats() RETURNS TABLE(tier text, device_count bigint)
    LANGUAGE plpgsql
    AS $$
BEGIN
  RETURN QUERY
  SELECT 
    CASE 
      WHEN (foss_count::float / NULLIF(total_apps, 0)) >= 0.8 THEN 'Sovereign'
      WHEN (foss_count::float / NULLIF(total_apps, 0)) >= 0.4 THEN 'Transitioning'
      ELSE 'Captured'
    END as sovereignty_tier,
    COUNT(*) as count
  FROM app_scan_stats
  WHERE total_apps > 0
  GROUP BY 1
  ORDER BY 1 DESC; -- 'S'overeign first, 'C'aptured last
END;
$$;


--
-- Name: handle_new_user(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.handle_new_user() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
begin
  insert into public.profiles (id, username)
  values (new.id, new.raw_user_meta_data->>'username'); -- Grabs username from sign-up metadata
  return new;
end;
$$;


--
-- Name: increment_submission_count(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.increment_submission_count() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
BEGIN
  UPDATE profiles 
  SET submission_count = COALESCE(submission_count, 0) + 1 
  WHERE id = NEW.submitter_id;
  RETURN NEW;
END;
$$;


--
-- Name: is_admin(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.is_admin() RETURNS boolean
    LANGUAGE sql STABLE SECURITY DEFINER
    SET search_path TO 'public'
    AS $$
  select exists (
    select 1
    from public.admin_users a
    where a.id = (select auth.uid())
  );
$$;


--
-- Name: is_first_admin_signup(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.is_first_admin_signup() RETURNS boolean
    LANGUAGE sql SECURITY DEFINER
    SET search_path TO 'public'
    AS $$
    SELECT NOT EXISTS (SELECT 1 FROM admin_users);
$$;


--
-- Name: update_submission_counts(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.update_submission_counts() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
BEGIN
  -- If status changed to APPROVED
  IF NEW.status = 'APPROVED' AND OLD.status != 'APPROVED' THEN
    UPDATE profiles 
    SET approved_count = COALESCE(approved_count, 0) + 1 
    WHERE id = NEW.submitter_id;
    
  -- If status changed to REJECTED
  ELSIF NEW.status = 'REJECTED' AND OLD.status != 'REJECTED' THEN
    UPDATE profiles 
    SET rejected_count = COALESCE(rejected_count, 0) + 1 
    WHERE id = NEW.submitter_id;
  END IF;
  
  RETURN NEW;
END;
$$;


--
-- Name: update_user_reputation(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.update_user_reputation() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
    current_score INTEGER;
BEGIN
    -- 1. Apply math only when status changes
    IF (OLD.status = 'PENDING' AND NEW.status = 'APPROVED') THEN
        UPDATE public.profiles 
        SET reputation_score = reputation_score + 10 
        WHERE id = NEW.submitter_id;
        
    ELSIF (OLD.status = 'PENDING' AND NEW.status = 'REJECTED') THEN
        -- GREATEST ensures score never drops below zero
        UPDATE public.profiles 
        SET reputation_score = GREATEST(reputation_score - 2, 0) 
        WHERE id = NEW.submitter_id;
    ELSE
        -- Exit early if status didn't change (Maximum efficiency)
        RETURN NEW;
    END IF;

    -- 2. Fetch the newly saved static score
    SELECT reputation_score INTO current_score
    FROM public.profiles
    WHERE id = NEW.submitter_id;

    -- 3. Update the Badge based on the new score
    UPDATE public.profiles 
    SET badge = CASE 
        WHEN current_score >= 500 THEN 'Curator'
        WHEN current_score >= 150 THEN 'Guide'
        ELSE 'Scout'
    END
    WHERE id = NEW.submitter_id;

    RETURN NEW;
END;
$$;


--
-- Name: upsert_app_technical_info(jsonb); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.upsert_app_technical_info(payload jsonb) RETURNS TABLE(inserted_or_updated bigint)
    LANGUAGE plpgsql
    AS $$
begin
  insert into app_technical_info (
    package_name, version_name, version_code, min_sdk_version,
    target_sdk_version, apk_size_bytes, permissions, native_code,
    added_at, updated_at
  )
  select
    (r->>'package_name')::text,
    (r->>'version_name')::text,
    (r->>'version_code')::bigint,
    (r->>'min_sdk_version')::int,
    (r->>'target_sdk_version')::int,
    (r->>'apk_size_bytes')::bigint,
    array(select jsonb_array_elements_text(r->'permissions')),
    array(select jsonb_array_elements_text(r->'native_code')),
    (r->>'added_at')::timestamptz,
    (r->>'updated_at')::timestamptz
  from jsonb_array_elements(payload) as r
  on conflict (package_name) do update set
    version_name = excluded.version_name,
    version_code = excluded.version_code,
    min_sdk_version = excluded.min_sdk_version,
    target_sdk_version = excluded.target_sdk_version,
    apk_size_bytes = excluded.apk_size_bytes,
    permissions = excluded.permissions,
    native_code = excluded.native_code,
    added_at = excluded.added_at,
    updated_at = excluded.updated_at
  where app_technical_info.version_code is distinct from excluded.version_code; -- skip no-op writes

  return query select count(*)::bigint from jsonb_array_elements(payload);
end;
$$;


--
-- Name: upsert_solution_guarded(jsonb); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.upsert_solution_guarded(payload jsonb) RETURNS void
    LANGUAGE plpgsql
    AS $$
declare
  incoming_priority int := coalesce((payload->>'source_priority')::int, 0);
begin
  insert into public.solutions (
    package_name, name, description, icon_url, fdroid_id, repo_url, license,
    fdroid_synced, category, source_repo, source_priority, last_synced_at
  )
  values (
    payload->>'package_name',
    payload->>'name',
    payload->>'description',
    payload->>'icon_url',
    payload->>'fdroid_id',
    payload->>'repo_url',
    payload->>'license',
    coalesce((payload->>'fdroid_synced')::boolean, false),
    payload->>'category',
    payload->>'source_repo',
    incoming_priority,
    coalesce((payload->>'last_synced_at')::timestamptz, now())
  )
  on conflict (package_name)
  do update
  set
    name = excluded.name,
    description = excluded.description,
    icon_url = excluded.icon_url,
    fdroid_id = excluded.fdroid_id,
    repo_url = excluded.repo_url,
    license = excluded.license,
    fdroid_synced = excluded.fdroid_synced,
    category = excluded.category,
    source_repo = excluded.source_repo,
    source_priority = excluded.source_priority,
    last_synced_at = excluded.last_synced_at
  where coalesce(public.solutions.source_priority, 0) <= excluded.source_priority;
end;
$$;


--
-- Name: upsert_solutions_guarded(jsonb); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.upsert_solutions_guarded(payload jsonb) RETURNS TABLE(inserted_or_updated integer)
    LANGUAGE sql
    AS $$
with rows as (
  select
    x.package_name::text as package_name,
    coalesce(x.name::text, x.package_name::text) as name,
    left(coalesce(x.description::text, ''), 1000) as description,
    coalesce(x.icon_url::text, '') as icon_url,
    coalesce(x.fdroid_id::text, x.package_name::text) as fdroid_id,
    coalesce(x.repo_url::text, '') as repo_url,
    coalesce(x.license::text, '') as license,
    coalesce(x.fdroid_synced::boolean, true) as fdroid_synced,
    nullif(x.category::text, '') as category,
    x.source_repo::text as source_repo,
    coalesce(x.source_priority::smallint, 0) as source_priority,
    coalesce(x.last_synced_at::timestamptz, now()) as last_synced_at
  from jsonb_to_recordset(payload) as x(
    package_name text,
    name text,
    description text,
    icon_url text,
    fdroid_id text,
    repo_url text,
    license text,
    fdroid_synced boolean,
    category text,
    source_repo text,
    source_priority smallint,
    last_synced_at timestamptz
  )
  where x.package_name is not null and x.package_name <> ''
),
upserted as (
  insert into public.solutions (
    package_name, name, description, icon_url, fdroid_id, repo_url, license,
    fdroid_synced, category, source_repo, source_priority, last_synced_at
  )
  select
    r.package_name, r.name, r.description, r.icon_url, r.fdroid_id, r.repo_url, r.license,
    r.fdroid_synced, r.category, r.source_repo, r.source_priority, r.last_synced_at
  from rows r
  on conflict (package_name) do update
    set
      name = excluded.name,
      description = excluded.description,
      icon_url = excluded.icon_url,
      fdroid_id = excluded.fdroid_id,
      repo_url = excluded.repo_url,
      license = excluded.license,
      fdroid_synced = excluded.fdroid_synced,
      category = excluded.category,
      source_repo = excluded.source_repo,
      source_priority = excluded.source_priority,
      last_synced_at = excluded.last_synced_at
    where coalesce(public.solutions.source_priority, 0) <= excluded.source_priority
  returning 1
)
select count(*)::int as inserted_or_updated from upserted;
$$;


--
-- Name: vote_for_app(text, text, integer); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.vote_for_app(package_name text, vote_type text, value integer) RETURNS void
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
declare
  v_privacy float;
  v_usability float;
  v_features float;
  v_count int;
begin
  -- Single aggregation query to calculate all ratings and count
  SELECT 
    COALESCE(AVG(CASE WHEN uv.vote_type = 'privacy' THEN uv.value END), 0)::float,
    COALESCE(AVG(CASE WHEN uv.vote_type = 'usability' THEN uv.value END), 0)::float,
    COALESCE(AVG(CASE WHEN uv.vote_type = 'features' THEN uv.value END), 0)::float,
    COUNT(DISTINCT uv.user_id)::int
  INTO v_privacy, v_usability, v_features, v_count
  FROM user_votes uv
  WHERE uv.package_name = vote_for_app.package_name;

  -- Single UPDATE with all calculated values
  UPDATE solutions
  SET 
    rating_privacy = v_privacy,
    rating_usability = v_usability,
    rating_features = v_features,
    vote_count = v_count
  WHERE solutions.package_name = vote_for_app.package_name;
end;
$$;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: admin_users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.admin_users (
    id uuid NOT NULL,
    email text NOT NULL,
    role text DEFAULT 'reviewer'::text NOT NULL,
    display_name text,
    created_at timestamp with time zone DEFAULT now(),
    created_by uuid,
    is_active boolean DEFAULT true,
    CONSTRAINT admin_users_role_check CHECK ((role = ANY (ARRAY['super_admin'::text, 'admin'::text, 'reviewer'::text])))
);


--
-- Name: app_corrections; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.app_corrections (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    created_at timestamp with time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    user_id uuid NOT NULL,
    package_name text NOT NULL,
    correction_type text NOT NULL,
    correction_value text NOT NULL,
    description text NOT NULL,
    status text DEFAULT 'PENDING'::text NOT NULL
);


--
-- Name: app_feedback; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.app_feedback (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    package_name text NOT NULL,
    feedback_type text NOT NULL,
    content text NOT NULL,
    submitter_id uuid,
    status text DEFAULT 'PENDING'::text NOT NULL,
    reviewed_at timestamp with time zone,
    reviewed_by uuid,
    rejection_reason text,
    CONSTRAINT app_feedback_feedback_type_check CHECK ((feedback_type = ANY (ARRAY['PRO'::text, 'CON'::text]))),
    CONSTRAINT app_feedback_status_check CHECK ((status = ANY (ARRAY['PENDING'::text, 'APPROVED'::text, 'REJECTED'::text])))
);


--
-- Name: TABLE app_feedback; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.app_feedback IS 'User-submitted PRO/CON feedback for apps, moderated by admins.';


--
-- Name: app_reports; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.app_reports (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    package_name text NOT NULL,
    issue_type text NOT NULL,
    description text,
    status text DEFAULT 'PENDING'::text,
    created_at timestamp with time zone DEFAULT now()
);


--
-- Name: app_scan_stats; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.app_scan_stats (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    device_id text NOT NULL,
    user_id uuid,
    foss_count integer DEFAULT 0 NOT NULL,
    proprietary_count integer DEFAULT 0 NOT NULL,
    unknown_count integer DEFAULT 0 NOT NULL,
    total_apps integer DEFAULT 0 NOT NULL,
    app_version text,
    scanned_at timestamp with time zone DEFAULT now(),
    pwa_count integer DEFAULT 0
);


--
-- Name: app_technical_info; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.app_technical_info (
    package_name text NOT NULL,
    version_name text,
    version_code bigint,
    min_sdk_version integer,
    target_sdk_version integer,
    apk_size_bytes bigint,
    permissions text[],
    native_code text[],
    added_at timestamp with time zone,
    updated_at timestamp with time zone DEFAULT now()
);


--
-- Name: comments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.comments (
    id uuid DEFAULT extensions.uuid_generate_v4() NOT NULL,
    target_id text NOT NULL,
    user_id uuid NOT NULL,
    content text NOT NULL,
    created_at timestamp with time zone DEFAULT now()
);


--
-- Name: moderation_actions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.moderation_actions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    performed_by uuid NOT NULL,
    action text NOT NULL,
    entity_type text NOT NULL,
    entity_table text NOT NULL,
    entity_id text NOT NULL,
    reason text,
    CONSTRAINT moderation_actions_action_check CHECK ((action = ANY (ARRAY['APPROVE'::text, 'REJECT'::text])))
);


--
-- Name: TABLE moderation_actions; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.moderation_actions IS 'Immutable audit log for admin/reviewer moderation actions (approve/reject).';


--
-- Name: profiles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.profiles (
    id uuid NOT NULL,
    username text,
    avatar_url text,
    created_at timestamp with time zone DEFAULT now(),
    submission_count integer DEFAULT 0,
    approved_count integer DEFAULT 0,
    rejected_count integer DEFAULT 0,
    reputation_score integer DEFAULT 0,
    badge text
);


--
-- Name: signing_key_votes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.signing_key_votes (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    package_name text NOT NULL,
    sha256_digest text NOT NULL,
    submitter_id uuid,
    created_at timestamp with time zone DEFAULT now(),
    app_label text
);


--
-- Name: solutions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.solutions (
    package_name text NOT NULL,
    name text NOT NULL,
    description text,
    fdroid_id text,
    repo_url text,
    website text,
    license text,
    icon_url text,
    pros text[],
    cons text[],
    features text[],
    created_at timestamp with time zone DEFAULT now(),
    rating_privacy double precision DEFAULT 0,
    rating_usability double precision DEFAULT 0,
    rating_features double precision DEFAULT 0,
    vote_count integer DEFAULT 0,
    fdroid_synced boolean DEFAULT false,
    category text DEFAULT 'Other'::text NOT NULL,
    source_repo text,
    last_synced_at timestamp with time zone,
    source_priority integer,
    CONSTRAINT solutions_source_repo_check CHECK ((source_repo = ANY (ARRAY['fdroid'::text, 'izzyondroid'::text, 'community'::text])))
);


--
-- Name: COLUMN solutions.fdroid_synced; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.solutions.fdroid_synced IS 'Indicates if this solution was synced from F-Droid repository';


--
-- Name: submission_votes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.submission_votes (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    submission_id text NOT NULL,
    submission_table text NOT NULL,
    user_id uuid NOT NULL,
    vote integer NOT NULL,
    reason text,
    reason_detail text,
    created_at timestamp with time zone DEFAULT now(),
    CONSTRAINT submission_votes_submission_table_check CHECK ((submission_table = ANY (ARRAY['user_submissions'::text, 'user_linking_submissions'::text]))),
    CONSTRAINT submission_votes_vote_check CHECK ((vote = ANY (ARRAY[1, '-1'::integer])))
);


--
-- Name: target_solution_votes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.target_solution_votes (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid,
    target_package text NOT NULL,
    solution_package text NOT NULL,
    vote smallint NOT NULL,
    created_at timestamp with time zone DEFAULT now(),
    CONSTRAINT target_solution_votes_vote_check CHECK ((vote = ANY (ARRAY[1, '-1'::integer])))
);


--
-- Name: targets; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.targets (
    package_name text NOT NULL,
    name text NOT NULL,
    category text,
    icon_url text,
    alternatives text[],
    created_at timestamp with time zone DEFAULT now(),
    description text
);


--
-- Name: user_linking_submissions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_linking_submissions (
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    proprietary_package text NOT NULL,
    alternatives text[] NOT NULL,
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    submitter_id uuid,
    status public.submission_status DEFAULT 'PENDING'::public.submission_status NOT NULL,
    rejection_reason text,
    last_edited_by uuid,
    last_edited_at timestamp with time zone,
    contributors uuid[] DEFAULT '{}'::uuid[]
);


--
-- Name: TABLE user_linking_submissions; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.user_linking_submissions IS 'User-submitted proprietary apps with alternatives';


--
-- Name: COLUMN user_linking_submissions.submitter_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_linking_submissions.submitter_id IS 'ID of the user';


--
-- Name: COLUMN user_linking_submissions.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_linking_submissions.status IS 'The status of the submission';


--
-- Name: user_reports; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_reports (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    title text NOT NULL,
    description text NOT NULL,
    report_type text NOT NULL,
    status text DEFAULT 'OPEN'::text NOT NULL,
    priority text DEFAULT 'LOW'::text,
    submitter_id uuid NOT NULL,
    admin_response text,
    resolved_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now(),
    CONSTRAINT user_reports_priority_check CHECK ((priority = ANY (ARRAY['LOW'::text, 'MEDIUM'::text, 'HIGH'::text, 'CRITICAL'::text]))),
    CONSTRAINT user_reports_report_type_check CHECK ((report_type = ANY (ARRAY['BUG'::text, 'SUGGESTION'::text, 'QUESTION'::text, 'FEEDBACK'::text, 'OTHER'::text]))),
    CONSTRAINT user_reports_status_check CHECK ((status = ANY (ARRAY['OPEN'::text, 'IN_PROGRESS'::text, 'RESOLVED'::text, 'WONTFIX'::text, 'DUPLICATE'::text, 'CLOSED'::text])))
);


--
-- Name: user_submissions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_submissions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    app_name text NOT NULL,
    app_package text NOT NULL,
    description text,
    fdroid_id text,
    license text,
    repo_url text,
    proprietary_package text,
    status text DEFAULT 'PENDING'::text,
    type text DEFAULT 'NEW_ALTERNATIVE'::text,
    submitter_id uuid,
    submitter_username text,
    submitted_at bigint,
    created_at timestamp with time zone DEFAULT now(),
    rejection_reason text,
    alternatives text[],
    submission_type text,
    category text,
    last_edited_by uuid,
    last_edited_at timestamp with time zone,
    contributors uuid[] DEFAULT '{}'::uuid[]
);


--
-- Name: COLUMN user_submissions.alternatives; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_submissions.alternatives IS 'Array of package names for suggested alternatives (for proprietary app submissions)';


--
-- Name: user_votes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_votes (
    user_id uuid NOT NULL,
    package_name text NOT NULL,
    vote_type text NOT NULL,
    value integer,
    CONSTRAINT user_votes_value_check CHECK (((value >= 1) AND (value <= 5))),
    CONSTRAINT user_votes_vote_type_check CHECK ((vote_type = ANY (ARRAY['usability'::text, 'privacy'::text, 'features'::text])))
);


--
-- Name: admin_users admin_users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_users
    ADD CONSTRAINT admin_users_pkey PRIMARY KEY (id);


--
-- Name: app_corrections app_corrections_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_corrections
    ADD CONSTRAINT app_corrections_pkey PRIMARY KEY (id);


--
-- Name: app_feedback app_feedback_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_feedback
    ADD CONSTRAINT app_feedback_pkey PRIMARY KEY (id);


--
-- Name: app_reports app_reports_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_reports
    ADD CONSTRAINT app_reports_pkey PRIMARY KEY (id);


--
-- Name: app_scan_stats app_scan_stats_device_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_scan_stats
    ADD CONSTRAINT app_scan_stats_device_id_key UNIQUE (device_id);


--
-- Name: app_scan_stats app_scan_stats_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_scan_stats
    ADD CONSTRAINT app_scan_stats_pkey PRIMARY KEY (id);


--
-- Name: app_technical_info app_technical_info_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_technical_info
    ADD CONSTRAINT app_technical_info_pkey PRIMARY KEY (package_name);


--
-- Name: comments comments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comments
    ADD CONSTRAINT comments_pkey PRIMARY KEY (id);


--
-- Name: moderation_actions moderation_actions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.moderation_actions
    ADD CONSTRAINT moderation_actions_pkey PRIMARY KEY (id);


--
-- Name: profiles profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.profiles
    ADD CONSTRAINT profiles_pkey PRIMARY KEY (id);


--
-- Name: profiles profiles_username_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.profiles
    ADD CONSTRAINT profiles_username_key UNIQUE (username);


--
-- Name: signing_key_votes signing_key_votes_package_name_submitter_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.signing_key_votes
    ADD CONSTRAINT signing_key_votes_package_name_submitter_id_key UNIQUE (package_name, submitter_id);


--
-- Name: signing_key_votes signing_key_votes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.signing_key_votes
    ADD CONSTRAINT signing_key_votes_pkey PRIMARY KEY (id);


--
-- Name: solutions solutions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.solutions
    ADD CONSTRAINT solutions_pkey PRIMARY KEY (package_name);


--
-- Name: submission_votes submission_votes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.submission_votes
    ADD CONSTRAINT submission_votes_pkey PRIMARY KEY (id);


--
-- Name: submission_votes submission_votes_submission_id_user_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.submission_votes
    ADD CONSTRAINT submission_votes_submission_id_user_id_key UNIQUE (submission_id, user_id);


--
-- Name: target_solution_votes target_solution_votes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.target_solution_votes
    ADD CONSTRAINT target_solution_votes_pkey PRIMARY KEY (id);


--
-- Name: target_solution_votes target_solution_votes_user_id_target_package_solution_packa_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.target_solution_votes
    ADD CONSTRAINT target_solution_votes_user_id_target_package_solution_packa_key UNIQUE (user_id, target_package, solution_package);


--
-- Name: targets targets_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.targets
    ADD CONSTRAINT targets_pkey PRIMARY KEY (package_name);


--
-- Name: user_reports user_reports_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_reports
    ADD CONSTRAINT user_reports_pkey PRIMARY KEY (id);


--
-- Name: user_submissions user_submissions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_submissions
    ADD CONSTRAINT user_submissions_pkey PRIMARY KEY (id);


--
-- Name: user_votes user_votes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_votes
    ADD CONSTRAINT user_votes_pkey PRIMARY KEY (user_id, package_name, vote_type);


--
-- Name: app_feedback_package_name_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX app_feedback_package_name_idx ON public.app_feedback USING btree (package_name);


--
-- Name: app_feedback_status_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX app_feedback_status_idx ON public.app_feedback USING btree (status);


--
-- Name: idx_admin_users_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_users_active ON public.admin_users USING btree (is_active);


--
-- Name: idx_admin_users_role; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_users_role ON public.admin_users USING btree (role);


--
-- Name: idx_profiles_reputation; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_profiles_reputation ON public.profiles USING btree (reputation_score DESC);


--
-- Name: idx_solutions_fdroid_synced; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_solutions_fdroid_synced ON public.solutions USING btree (fdroid_synced);


--
-- Name: idx_solutions_last_synced_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_solutions_last_synced_at ON public.solutions USING btree (last_synced_at);


--
-- Name: idx_solutions_package_name; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_solutions_package_name ON public.solutions USING btree (package_name);


--
-- Name: idx_solutions_source_priority; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_solutions_source_priority ON public.solutions USING btree (source_priority);


--
-- Name: idx_solutions_source_repo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_solutions_source_repo ON public.solutions USING btree (source_repo);


--
-- Name: idx_targets_package_name; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_targets_package_name ON public.targets USING btree (package_name);


--
-- Name: idx_user_linking_submissions_last_edited_by; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_linking_submissions_last_edited_by ON public.user_linking_submissions USING btree (last_edited_by);


--
-- Name: idx_user_submissions_last_edited_by; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_submissions_last_edited_by ON public.user_submissions USING btree (last_edited_by);


--
-- Name: idx_user_submissions_submitter; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_submissions_submitter ON public.user_submissions USING btree (submitter_id);


--
-- Name: idx_user_votes_composite; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_votes_composite ON public.user_votes USING btree (user_id, package_name, vote_type);


--
-- Name: idx_user_votes_package_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_votes_package_type ON public.user_votes USING btree (package_name, vote_type);


--
-- Name: ix_solutions_last_synced_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_solutions_last_synced_at ON public.solutions USING btree (last_synced_at DESC);


--
-- Name: ix_solutions_source_repo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_solutions_source_repo ON public.solutions USING btree (source_repo);


--
-- Name: ix_solutions_source_repo_synced; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_solutions_source_repo_synced ON public.solutions USING btree (source_repo, fdroid_synced);


--
-- Name: moderation_actions_created_at_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX moderation_actions_created_at_idx ON public.moderation_actions USING btree (created_at);


--
-- Name: moderation_actions_entity_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX moderation_actions_entity_idx ON public.moderation_actions USING btree (entity_table, entity_id);


--
-- Name: moderation_actions_performed_by_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX moderation_actions_performed_by_idx ON public.moderation_actions USING btree (performed_by);


--
-- Name: profiles_approved_count_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX profiles_approved_count_idx ON public.profiles USING btree (approved_count);


--
-- Name: user_submissions_created_at_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX user_submissions_created_at_idx ON public.user_submissions USING btree (created_at);


--
-- Name: ux_solutions_package_name; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX ux_solutions_package_name ON public.solutions USING btree (package_name);


--
-- Name: signing_key_votes on_signing_key_vote_insert; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER on_signing_key_vote_insert AFTER INSERT ON public.signing_key_votes FOR EACH ROW EXECUTE FUNCTION public.check_signing_key_threshold();


--
-- Name: user_linking_submissions tr_increment_submission_count_links; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER tr_increment_submission_count_links AFTER INSERT ON public.user_linking_submissions FOR EACH ROW EXECUTE FUNCTION public.increment_submission_count();


--
-- Name: user_submissions tr_increment_submission_count_subs; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER tr_increment_submission_count_subs AFTER INSERT ON public.user_submissions FOR EACH ROW EXECUTE FUNCTION public.increment_submission_count();


--
-- Name: user_linking_submissions tr_update_submission_counts_links; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER tr_update_submission_counts_links AFTER UPDATE ON public.user_linking_submissions FOR EACH ROW EXECUTE FUNCTION public.update_submission_counts();


--
-- Name: user_submissions tr_update_submission_counts_subs; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER tr_update_submission_counts_subs AFTER UPDATE ON public.user_submissions FOR EACH ROW EXECUTE FUNCTION public.update_submission_counts();


--
-- Name: submission_votes trigger_auto_approve_submission; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trigger_auto_approve_submission AFTER INSERT OR UPDATE ON public.submission_votes FOR EACH ROW EXECUTE FUNCTION public.check_submission_auto_approval();


--
-- Name: admin_users admin_users_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_users
    ADD CONSTRAINT admin_users_created_by_fkey FOREIGN KEY (created_by) REFERENCES auth.users(id);


--
-- Name: admin_users admin_users_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_users
    ADD CONSTRAINT admin_users_id_fkey FOREIGN KEY (id) REFERENCES auth.users(id) ON DELETE CASCADE;


--
-- Name: app_corrections app_corrections_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_corrections
    ADD CONSTRAINT app_corrections_user_id_fkey FOREIGN KEY (user_id) REFERENCES auth.users(id);


--
-- Name: app_feedback app_feedback_reviewed_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_feedback
    ADD CONSTRAINT app_feedback_reviewed_by_fkey FOREIGN KEY (reviewed_by) REFERENCES auth.users(id) ON DELETE SET NULL;


--
-- Name: app_feedback app_feedback_submitter_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_feedback
    ADD CONSTRAINT app_feedback_submitter_id_fkey FOREIGN KEY (submitter_id) REFERENCES auth.users(id) ON DELETE SET NULL;


--
-- Name: app_reports app_reports_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_reports
    ADD CONSTRAINT app_reports_user_id_fkey FOREIGN KEY (user_id) REFERENCES auth.users(id);


--
-- Name: app_scan_stats app_scan_stats_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_scan_stats
    ADD CONSTRAINT app_scan_stats_user_id_fkey FOREIGN KEY (user_id) REFERENCES auth.users(id);


--
-- Name: app_technical_info app_technical_info_package_name_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_technical_info
    ADD CONSTRAINT app_technical_info_package_name_fkey FOREIGN KEY (package_name) REFERENCES public.solutions(package_name) ON DELETE CASCADE;


--
-- Name: comments comments_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comments
    ADD CONSTRAINT comments_user_id_fkey FOREIGN KEY (user_id) REFERENCES auth.users(id);

ALTER TABLE ONLY public.comments
    ADD CONSTRAINT comments_profile_id_fkey FOREIGN KEY (user_id) REFERENCES public.profiles(id);


--
-- Name: user_submissions fk_submissions_profiles; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_submissions
    ADD CONSTRAINT fk_submissions_profiles FOREIGN KEY (submitter_id) REFERENCES public.profiles(id);


--
-- Name: moderation_actions moderation_actions_performed_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.moderation_actions
    ADD CONSTRAINT moderation_actions_performed_by_fkey FOREIGN KEY (performed_by) REFERENCES auth.users(id) ON DELETE CASCADE;


--
-- Name: profiles profiles_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.profiles
    ADD CONSTRAINT profiles_id_fkey FOREIGN KEY (id) REFERENCES auth.users(id) ON DELETE CASCADE;


--
-- Name: signing_key_votes signing_key_votes_submitter_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.signing_key_votes
    ADD CONSTRAINT signing_key_votes_submitter_id_fkey FOREIGN KEY (submitter_id) REFERENCES public.profiles(id);


--
-- Name: submission_votes submission_votes_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.submission_votes
    ADD CONSTRAINT submission_votes_user_id_fkey FOREIGN KEY (user_id) REFERENCES auth.users(id);


--
-- Name: target_solution_votes target_solution_votes_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.target_solution_votes
    ADD CONSTRAINT target_solution_votes_user_id_fkey FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE;


--
-- Name: user_linking_submissions user_linking_submissions_last_edited_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_linking_submissions
    ADD CONSTRAINT user_linking_submissions_last_edited_by_fkey FOREIGN KEY (last_edited_by) REFERENCES public.profiles(id);


--
-- Name: user_linking_submissions user_linking_submissions_submitter_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_linking_submissions
    ADD CONSTRAINT user_linking_submissions_submitter_id_fkey FOREIGN KEY (submitter_id) REFERENCES public.profiles(id);


--
-- Name: user_reports user_reports_submitter_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_reports
    ADD CONSTRAINT user_reports_submitter_id_fkey FOREIGN KEY (submitter_id) REFERENCES public.profiles(id);


--
-- Name: user_submissions user_submissions_last_edited_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_submissions
    ADD CONSTRAINT user_submissions_last_edited_by_fkey FOREIGN KEY (last_edited_by) REFERENCES public.profiles(id);


--
-- Name: user_votes user_votes_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_votes
    ADD CONSTRAINT user_votes_user_id_fkey FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE;


--
-- Name: solutions Admin users can insert solutions; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY "Admin users can insert solutions" ON public.solutions FOR INSERT TO authenticated WITH CHECK ((EXISTS ( SELECT 1
   FROM public.admin_users
  WHERE ((admin_users.id = auth.uid()) AND (admin_users.is_active = true)))));


--
-- Name: targets Admin users can insert targets; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY "Admin users can insert targets" ON public.targets FOR INSERT TO authenticated WITH CHECK ((EXISTS ( SELECT 1
   FROM public.admin_users
  WHERE ((admin_users.id = auth.uid()) AND (admin_users.is_active = true)))));


--
-- Name: user_reports Admin users can update reports; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY "Admin users can update reports" ON public.user_reports FOR UPDATE TO authenticated USING ((EXISTS ( SELECT 1
   FROM public.admin_users
  WHERE ((admin_users.id = auth.uid()) AND (admin_users.is_active = true)))));


--
-- Name: solutions Admin users can update solutions; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY "Admin users can update solutions" ON public.solutions FOR UPDATE TO authenticated USING ((EXISTS ( SELECT 1
   FROM public.admin_users
  WHERE ((admin_users.id = auth.uid()) AND (admin_users.is_active = true)))));


--
-- Name: user_submissions Admin users can update submission status; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY "Admin users can update submission status" ON public.user_submissions FOR UPDATE TO authenticated USING ((EXISTS ( SELECT 1
   FROM public.admin_users
  WHERE ((admin_users.id = auth.uid()) AND (admin_users.is_active = true)))));


--
-- Name: targets Admin users can update targets; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY "Admin users can update targets" ON public.targets FOR UPDATE TO authenticated USING ((EXISTS ( SELECT 1
   FROM public.admin_users
  WHERE ((admin_users.id = auth.uid()) AND (admin_users.is_active = true)))));


--
-- Name: user_reports Admin users can view all reports; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY "Admin users can view all reports" ON public.user_reports FOR SELECT TO authenticated USING (((auth.uid() = submitter_id) OR (EXISTS ( SELECT 1
   FROM public.admin_users
  WHERE ((admin_users.id = auth.uid()) AND (admin_users.is_active = true))))));


--
-- Name: user_submissions Admin users can view all submissions; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY "Admin users can view all submissions" ON public.user_submissions FOR SELECT TO authenticated USING (((auth.uid() = submitter_id) OR (EXISTS ( SELECT 1
   FROM public.admin_users
  WHERE ((admin_users.id = auth.uid()) AND (admin_users.is_active = true))))));


--
-- Name: user_linking_submissions Admins can update linking submission; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY "Admins can update linking submission" ON public.user_linking_submissions FOR UPDATE USING ((EXISTS ( SELECT 1
   FROM public.admin_users
  WHERE ((admin_users.id = auth.uid()) AND (admin_users.is_active = true)))));


--
-- Name: user_linking_submissions Admins can update linking submissions; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY "Admins can update linking submissions" ON public.user_linking_submissions FOR UPDATE USING ((EXISTS ( SELECT 1
   FROM public.admin_users
  WHERE ((admin_users.id = auth.uid()) AND (admin_users.is_active = true)))));


--
-- Name: user_reports Admins can update reports; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY "Admins can update reports" ON public.user_reports FOR UPDATE TO authenticated USING (((auth.jwt() ->> 'user_role'::text) = 'admin'::text)) WITH CHECK (((auth.jwt() ->> 'user_role'::text) = 'admin'::text));


--
-- Name: user_linking_submissions Allow admins to update; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY "Allow admins to update" ON public.user_linking_submissions FOR UPDATE TO authenticated USING (true);


--
-- Name: app_scan_stats Allow anonymous stats; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY "Allow anonymous stats" ON public.app_scan_stats USING (true) WITH CHECK (true);


--
-- Name: comments Anyone can read comments; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY "Anyone can read comments" ON public.comments FOR SELECT USING (true);


--
-- Name: user_submissions Auth Create Submissions; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY "Auth Create Submissions" ON public.user_submissions FOR INSERT TO authenticated WITH CHECK (true);


--
-- Name: comments Authenticated users can insert comments; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY "Authenticated users can insert comments" ON public.comments FOR INSERT WITH CHECK ((auth.uid() = user_id));


--
-- Name: signing_key_votes Authenticated users can submit signing key vote; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY "Authenticated users can submit signing key vote" ON public.signing_key_votes FOR INSERT TO authenticated WITH CHECK ((submitter_id = auth.uid()));


--
-- Name: user_submissions Enable all access for now; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY "Enable all access for now" ON public.user_submissions USING (true) WITH CHECK (true);


--
-- Name: user_linking_submissions Enable insert for authenticated users only; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY "Enable insert for authenticated users only" ON public.user_linking_submissions FOR INSERT TO authenticated WITH CHECK ((auth.uid() = submitter_id));


--
-- Name: admin_users First user or super admin can insert; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY "First user or super admin can insert" ON public.admin_users FOR INSERT TO authenticated WITH CHECK ((((id = auth.uid()) AND public.is_first_admin_signup()) OR (EXISTS ( SELECT 1
   FROM public.admin_users admin_users_1
  WHERE ((admin_users_1.id = auth.uid()) AND (admin_users_1.role = 'super_admin'::text) AND (admin_users_1.is_active = true))))));


--
-- Name: solutions Public Read; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY "Public Read" ON public.solutions FOR SELECT USING (true);


--
-- Name: profiles Public Read Profiles; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY "Public Read Profiles" ON public.profiles FOR SELECT USING (true);


--
-- Name: solutions Public Read Solutions; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY "Public Read Solutions" ON public.solutions FOR SELECT USING (true);


--
-- Name: user_submissions Public Read Submissions; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY "Public Read Submissions" ON public.user_submissions FOR SELECT USING (true);


--
-- Name: targets Public Read Targets; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY "Public Read Targets" ON public.targets FOR SELECT USING (true);


--
-- Name: signing_key_votes Public read signing key votes; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY "Public read signing key votes" ON public.signing_key_votes FOR SELECT USING (true);


--
-- Name: admin_users Super admins can delete admin users; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY "Super admins can delete admin users" ON public.admin_users FOR DELETE TO authenticated USING ((EXISTS ( SELECT 1
   FROM public.admin_users admin_users_1
  WHERE ((admin_users_1.id = auth.uid()) AND (admin_users_1.role = 'super_admin'::text) AND (admin_users_1.is_active = true)))));


--
-- Name: admin_users Super admins can update admin users; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY "Super admins can update admin users" ON public.admin_users FOR UPDATE TO authenticated USING ((EXISTS ( SELECT 1
   FROM public.admin_users admin_users_1
  WHERE ((admin_users_1.id = auth.uid()) AND (admin_users_1.role = 'super_admin'::text) AND (admin_users_1.is_active = true)))));


--
-- Name: user_votes Users Manage Own Votes; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY "Users Manage Own Votes" ON public.user_votes USING ((auth.uid() = user_id));


--
-- Name: profiles Users Update Own Profile; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY "Users Update Own Profile" ON public.profiles FOR UPDATE USING ((auth.uid() = id));


--
-- Name: signing_key_votes Users can delete their own signing key vote; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY "Users can delete their own signing key vote" ON public.signing_key_votes FOR DELETE TO authenticated USING ((submitter_id = auth.uid()));


--
-- Name: user_reports Users can insert own reports; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY "Users can insert own reports" ON public.user_reports FOR INSERT WITH CHECK ((auth.uid() = submitter_id));


--
-- Name: app_corrections Users can insert their own corrections; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY "Users can insert their own corrections" ON public.app_corrections FOR INSERT WITH CHECK ((auth.uid() = user_id));


--
-- Name: target_solution_votes Users can manage own match votes; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY "Users can manage own match votes" ON public.target_solution_votes USING ((auth.uid() = user_id));


--
-- Name: admin_users Users can read own record or if admin; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY "Users can read own record or if admin" ON public.admin_users FOR SELECT TO authenticated USING (((id = auth.uid()) OR (EXISTS ( SELECT 1
   FROM public.admin_users au
  WHERE ((au.id = auth.uid()) AND (au.is_active = true))))));


--
-- Name: app_reports Users can report issues; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY "Users can report issues" ON public.app_reports FOR INSERT TO authenticated WITH CHECK (true);


--
-- Name: signing_key_votes Users can update their own signing key vote; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY "Users can update their own signing key vote" ON public.signing_key_votes FOR UPDATE TO authenticated USING ((submitter_id = auth.uid())) WITH CHECK ((submitter_id = auth.uid()));


--
-- Name: app_reports Users can view own reports; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY "Users can view own reports" ON public.app_reports FOR SELECT TO authenticated USING ((auth.uid() = user_id));


--
-- Name: user_reports Users can view own reports; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY "Users can view own reports" ON public.user_reports FOR SELECT USING ((auth.uid() = submitter_id));


--
-- Name: app_corrections Users can view their own corrections; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY "Users can view their own corrections" ON public.app_corrections FOR SELECT USING ((auth.uid() = user_id));


--
-- Name: user_linking_submissions Users can view their own linking submissions; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY "Users can view their own linking submissions" ON public.user_linking_submissions FOR SELECT TO authenticated USING ((auth.uid() = submitter_id));


--
-- Name: app_corrections; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.app_corrections ENABLE ROW LEVEL SECURITY;

--
-- Name: app_feedback; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.app_feedback ENABLE ROW LEVEL SECURITY;

--
-- Name: app_feedback app_feedback_admin_delete; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY app_feedback_admin_delete ON public.app_feedback FOR DELETE TO authenticated USING ((((auth.jwt() -> 'app_metadata'::text) ->> 'role'::text) = ANY (ARRAY['admin'::text, 'superadmin'::text])));


--
-- Name: app_feedback app_feedback_admin_select; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY app_feedback_admin_select ON public.app_feedback FOR SELECT TO authenticated USING ((((auth.jwt() -> 'app_metadata'::text) ->> 'role'::text) = ANY (ARRAY['admin'::text, 'superadmin'::text])));


--
-- Name: app_feedback app_feedback_admin_update; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY app_feedback_admin_update ON public.app_feedback FOR UPDATE TO authenticated USING (public.is_admin()) WITH CHECK (public.is_admin());


--
-- Name: app_feedback app_feedback_insert_authenticated; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY app_feedback_insert_authenticated ON public.app_feedback FOR INSERT TO authenticated WITH CHECK ((submitter_id = auth.uid()));


--
-- Name: app_feedback app_feedback_select_all_authenticated_tmp; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY app_feedback_select_all_authenticated_tmp ON public.app_feedback FOR SELECT TO authenticated USING (true);


--
-- Name: app_feedback app_feedback_select_own; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY app_feedback_select_own ON public.app_feedback FOR SELECT TO authenticated USING ((submitter_id = auth.uid()));


--
-- Name: app_reports; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.app_reports ENABLE ROW LEVEL SECURITY;

--
-- Name: app_scan_stats; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.app_scan_stats ENABLE ROW LEVEL SECURITY;

--
-- Name: app_technical_info; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.app_technical_info ENABLE ROW LEVEL SECURITY;

--
-- Name: comments; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.comments ENABLE ROW LEVEL SECURITY;

--
-- Name: user_linking_submissions linking_admin_select; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY linking_admin_select ON public.user_linking_submissions FOR SELECT TO authenticated USING (public.is_admin());


--
-- Name: user_linking_submissions linking_admin_update; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY linking_admin_update ON public.user_linking_submissions FOR UPDATE TO authenticated USING (public.is_admin()) WITH CHECK (public.is_admin());


--
-- Name: moderation_actions; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.moderation_actions ENABLE ROW LEVEL SECURITY;

--
-- Name: moderation_actions moderation_actions_admin_insert; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY moderation_actions_admin_insert ON public.moderation_actions FOR INSERT TO authenticated WITH CHECK ((public.is_admin() AND (performed_by = ( SELECT auth.uid() AS uid))));


--
-- Name: moderation_actions moderation_actions_admin_select; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY moderation_actions_admin_select ON public.moderation_actions FOR SELECT TO authenticated USING (public.is_admin());


--
-- Name: profiles; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

--
-- Name: signing_key_votes; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.signing_key_votes ENABLE ROW LEVEL SECURITY;

--
-- Name: solutions; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.solutions ENABLE ROW LEVEL SECURITY;

--
-- Name: submission_votes; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.submission_votes ENABLE ROW LEVEL SECURITY;

--
-- Name: target_solution_votes; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.target_solution_votes ENABLE ROW LEVEL SECURITY;

--
-- Name: targets; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.targets ENABLE ROW LEVEL SECURITY;

--
-- Name: user_linking_submissions; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.user_linking_submissions ENABLE ROW LEVEL SECURITY;

--
-- Name: user_reports; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.user_reports ENABLE ROW LEVEL SECURITY;

--
-- Name: user_submissions; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.user_submissions ENABLE ROW LEVEL SECURITY;

--
-- Name: user_votes; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.user_votes ENABLE ROW LEVEL SECURITY;

--
-- Name: submission_votes users can manage own submission votes; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY "users can manage own submission votes" ON public.submission_votes USING ((auth.uid() = user_id)) WITH CHECK ((auth.uid() = user_id));


--
-- Name: submission_votes votes are publicly readable; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY "votes are publicly readable" ON public.submission_votes FOR SELECT USING (true);


--
-- PostgreSQL database dump complete
--

\unrestrict yqFZgGi6U41h2bOZ9lrnvA4WWunusMrrTwheZzuAIpIhd7XuulHlgLQrcLdYJhx

