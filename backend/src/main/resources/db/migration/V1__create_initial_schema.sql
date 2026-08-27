CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_email_normalized CHECK (email = lower(email))
);

CREATE TABLE refresh_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    rotated_to UUID,
    CONSTRAINT uk_refresh_sessions_token UNIQUE (token_hash),
    CONSTRAINT fk_refresh_rotated_to FOREIGN KEY (rotated_to) REFERENCES refresh_sessions(id)
);
CREATE INDEX ix_refresh_sessions_user_active ON refresh_sessions(user_id, expires_at) WHERE revoked_at IS NULL;

CREATE TABLE households (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    timezone VARCHAR(64) NOT NULL DEFAULT 'America/Maceio',
    currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_households_currency CHECK (currency = 'BRL')
);

CREATE TABLE household_members (
    id UUID PRIMARY KEY,
    household_id UUID NOT NULL REFERENCES households(id),
    user_id UUID NOT NULL REFERENCES users(id),
    role VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL,
    removed_at TIMESTAMPTZ,
    CONSTRAINT uk_household_membership UNIQUE (household_id, user_id),
    CONSTRAINT ck_household_member_role CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER')),
    CONSTRAINT ck_household_member_status CHECK (status IN ('ACTIVE', 'REMOVED'))
);
CREATE INDEX ix_household_members_user_status ON household_members(user_id, status);

CREATE TABLE invitations (
    id UUID PRIMARY KEY,
    household_id UUID NOT NULL REFERENCES households(id),
    invited_by UUID NOT NULL REFERENCES users(id),
    role VARCHAR(16) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    used_by UUID REFERENCES users(id),
    CONSTRAINT uk_invitations_token UNIQUE (token_hash),
    CONSTRAINT ck_invitation_role CHECK (role IN ('ADMIN', 'MEMBER'))
);
CREATE INDEX ix_invitations_household ON invitations(household_id, expires_at);

CREATE TABLE expenses (
    id UUID PRIMARY KEY,
    household_id UUID NOT NULL REFERENCES households(id),
    created_by_member_id UUID NOT NULL REFERENCES household_members(id),
    paid_by_member_id UUID REFERENCES household_members(id),
    title VARCHAR(120) NOT NULL,
    total NUMERIC(19,2) NOT NULL,
    category VARCHAR(40) NOT NULL,
    due_date DATE NOT NULL,
    notes VARCHAR(500),
    split_type VARCHAR(16) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    cancelled_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_expenses_total CHECK (total > 0 AND scale(total) <= 2),
    CONSTRAINT ck_expenses_split_type CHECK (split_type IN ('EQUAL', 'CUSTOM')),
    CONSTRAINT ck_expenses_currency CHECK (currency = 'BRL'),
    CONSTRAINT ck_expenses_status CHECK (status IN ('PENDING', 'SETTLED', 'CANCELLED'))
);
CREATE INDEX ix_expenses_household_due ON expenses(household_id, due_date DESC);
CREATE INDEX ix_expenses_household_status ON expenses(household_id, status);
CREATE INDEX ix_expenses_household_category ON expenses(household_id, category);

CREATE TABLE expense_shares (
    id UUID PRIMARY KEY,
    expense_id UUID NOT NULL REFERENCES expenses(id),
    member_id UUID NOT NULL REFERENCES household_members(id),
    amount NUMERIC(19,2) NOT NULL,
    status VARCHAR(16) NOT NULL,
    settled_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_expense_share_member UNIQUE (expense_id, member_id),
    CONSTRAINT ck_expense_share_amount CHECK (amount > 0 AND scale(amount) <= 2),
    CONSTRAINT ck_expense_share_status CHECK (status IN ('PENDING', 'COVERED', 'SETTLED'))
);
CREATE INDEX ix_expense_shares_member_status ON expense_shares(member_id, status);

CREATE TABLE settlements (
    id UUID PRIMARY KEY,
    expense_id UUID NOT NULL REFERENCES expenses(id),
    share_id UUID REFERENCES expense_shares(id),
    household_id UUID NOT NULL REFERENCES households(id),
    actor_user_id UUID NOT NULL REFERENCES users(id),
    payer_member_id UUID NOT NULL REFERENCES household_members(id),
    recipient_member_id UUID REFERENCES household_members(id),
    amount NUMERIC(19,2) NOT NULL,
    type VARCHAR(24) NOT NULL,
    reverses_settlement_id UUID REFERENCES settlements(id),
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_settlement_amount CHECK (amount > 0 AND scale(amount) <= 2),
    CONSTRAINT ck_settlement_type CHECK (type IN ('PRIMARY_PAYMENT', 'SHARE_PAYMENT', 'REIMBURSEMENT', 'REVERSAL'))
);
CREATE INDEX ix_settlements_expense_time ON settlements(expense_id, occurred_at);

CREATE TABLE idempotency_records (
    id UUID PRIMARY KEY,
    household_id UUID NOT NULL REFERENCES households(id),
    actor_user_id UUID NOT NULL REFERENCES users(id),
    operation VARCHAR(40) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    settlement_id UUID NOT NULL REFERENCES settlements(id),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_idempotency_scope UNIQUE (household_id, actor_user_id, operation, idempotency_key)
);

CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    household_id UUID NOT NULL REFERENCES households(id),
    expense_id UUID REFERENCES expenses(id),
    actor_user_id UUID NOT NULL REFERENCES users(id),
    event_type VARCHAR(40) NOT NULL,
    details JSONB NOT NULL DEFAULT '{}'::jsonb,
    occurred_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_audit_events_expense_time ON audit_events(expense_id, occurred_at);
CREATE INDEX ix_audit_events_household_time ON audit_events(household_id, occurred_at DESC);
