ALTER TABLE expense_shares ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE expense_shares DROP CONSTRAINT uk_expense_share_member;
CREATE UNIQUE INDEX uk_active_expense_share_member
    ON expense_shares (expense_id, member_id) WHERE active;

CREATE UNIQUE INDEX uk_primary_payment_expense
    ON settlements (expense_id) WHERE type = 'PRIMARY_PAYMENT';
CREATE UNIQUE INDEX uk_settlement_share
    ON settlements (share_id) WHERE type IN ('SHARE_PAYMENT', 'REIMBURSEMENT');
