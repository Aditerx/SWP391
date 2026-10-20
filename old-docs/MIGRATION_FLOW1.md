# Flow 1 database rollout

For an existing PostgreSQL database, apply these scripts in order, then deploy the matching backend:

1. `migration_flow1_step1.sql` — login failure counters and temporary lock timestamp.
2. `migration_flow1_step2.sql` — centers, center assignments, and center IDs for rooms/classes/packages.
3. `migration_flow1_step3.sql` — first-login password flag.
4. Step 4 adds no database objects.
5. `migration_flow1_step5.sql` — OTP storage.
6. `migration_flow1_step6.sql` — allow Pending user status.
7. `migration_flow1_step7.sql` — public subject fields, coach specializations, and the two new Admin permissions.
8. `migration_flow1_step8.sql` — avatar metadata.

`schema_postgres.sql` includes the same Flow 1 schema for fresh PostgreSQL installs. SMTP and Cloudinary credentials are read from environment variables or the ignored local `.env`; `.env.example` only documents the variable names.
