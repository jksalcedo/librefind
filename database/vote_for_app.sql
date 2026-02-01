-- Multi-dimensional rating RPC function for LibreFind
-- Optimized version: Uses single aggregation query instead of 4 subqueries
-- Vote types: 'usability', 'privacy', 'features'
-- Each vote is 1-5 stars

create or replace function vote_for_app(
  package_name text,
  vote_type text,
  value int
)
returns void as $$
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
$$ language plpgsql security definer;
