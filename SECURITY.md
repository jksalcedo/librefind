# Security Policy

## Supported Versions

Only the latest release and the `main` branch of LibreFind are actively supported with security updates. 

* **Latest Release** (Codeberg / F-Droid)
* **`main` branch**

## How To Report a Vulnerability

If you believe you have found a security vulnerability in LibreFind, please report it privately through coordinated disclosure. 

**Please do not report security vulnerabilities through public issues, discussions, or pull requests on Codeberg or GitHub.**

Instead, report it using one of the following methods:
* **Email:** jksalcedo_dev@disroot.org
* **GitHub Private Reporting:** If you maintain a mirror on GitHub, you can report it via [private vulnerability reporting](https://github.com/jksalcedo/librefind/security/advisories/new).

Please include as much of the information listed below as you can to help resolve the issue:
* The type of issue (e.g., access control evasion, insecure local storage, SQL injection)
* Affected version(s)
* Impact of the issue, including how an attacker might exploit it
* Step-by-step instructions to reproduce the issue
* The location of the affected source code (tag/branch/commit)
* Proof-of-concept or exploit code (if possible)

## Rules of Engagement & Bug Bounties

LibreFind is an independent, volunteer-driven FOSS project. By conducting security research on this project, you agree to the following terms:
1. **Zero Financial Expectation:** No financial bounties will be paid for vulnerability reports. Public credit and acknowledgment will be provided in the repository and release notes for valid findings.
2. **Testing Scope:** You are permitted to audit the open-source repository and test local, self-hosted builds. **You are strictly prohibited from running automated vulnerability scanners, penetration testing tools, or stress tests against the live production backend (Supabase).**
3. **Data Handling:** If you discover an access or authorization vulnerability, you must stop testing immediately. You may not download, copy, exfiltrate, or store any live user data.