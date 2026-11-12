-- ============================================================================
-- 0002_registration_requests.sql
--
-- Adds self-service account *requests*. This system holds fleet, driver,
-- salary and audit data, so there is deliberately no open registration: a
-- visitor may submit a request, but an administrator must approve it before
-- an account exists or can be signed in to.
--
-- Least privilege: the requester may state a role preference, but the account
-- is created with the role the administrator chooses at approval time, and
-- the table defaults to VIEWER so an unattended approval cannot grant more.
-- ============================================================================

create table if not exists registration_requests (
    id             bigint generated always as identity primary key,
    username       text not null,
    email          text not null,
    full_name      text not null,
    requested_role text not null default 'VIEWER'
                   check (requested_role in ('ADMINISTRATOR','FLEET_MANAGER','OPERATIONS_OFFICER','VIEWER')),
    department     text,
    reason         text,
    status         text not null default 'PENDING' check (status in ('PENDING','APPROVED','REJECTED')),
    decided_by     text,
    decided_at     timestamptz,
    decision_note  text,
    created_user_id bigint references users (id) on delete set null,
    created_at     timestamptz not null default now(),
    updated_at     timestamptz not null default now()
);

create index if not exists idx_registration_status  on registration_requests (status);
create index if not exists idx_registration_created on registration_requests (created_at desc);

-- At most one open request per username or e-mail, case-insensitively. Repeat
-- submissions become "updates" of the pending row rather than duplicates.
create unique index if not exists uq_registration_pending_username
    on registration_requests (lower(username)) where status = 'PENDING';
create unique index if not exists uq_registration_pending_email
    on registration_requests (lower(email))    where status = 'PENDING';

-- Same access model as every other table: the browser never talks to this
-- database directly, and RLS with no policies denies anon/publishable keys.
alter table registration_requests enable row level security;

-- Keep updated_at honest, reusing the existing trigger function.
drop trigger if exists trg_touch_registration_requests on registration_requests;
create trigger trg_touch_registration_requests
    before update on registration_requests
    for each row execute function touch_updated_at();
