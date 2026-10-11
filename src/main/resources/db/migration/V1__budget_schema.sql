CREATE SEQUENCE member_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE budget_plan_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE category_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE spend_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE notification_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE app_member (
    id NUMBER(19) PRIMARY KEY,
    email VARCHAR2(254 CHAR) NOT NULL,
    password_hash VARCHAR2(100 CHAR) NOT NULL,
    name VARCHAR2(50 CHAR) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_member_email UNIQUE (email)
);

CREATE TABLE budget_plan (
    id NUMBER(19) PRIMARY KEY,
    member_id NUMBER(19) NOT NULL,
    ym VARCHAR2(7 CHAR) NOT NULL,
    amount NUMBER(14,0) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_plan_member FOREIGN KEY (member_id) REFERENCES app_member(id),
    CONSTRAINT uk_plan_member_month UNIQUE (member_id, ym),
    CONSTRAINT ck_plan_amount CHECK (amount > 0),
    CONSTRAINT ck_plan_ym CHECK (REGEXP_LIKE(ym, '^[1-9][0-9]{3}-(0[1-9]|1[0-2])$'))
);

CREATE TABLE category (
    id NUMBER(19) PRIMARY KEY,
    member_id NUMBER(19),
    name VARCHAR2(50 CHAR) NOT NULL,
    normalized_name VARCHAR2(50 CHAR) NOT NULL,
    active NUMBER(1) DEFAULT 1 NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_category_member FOREIGN KEY (member_id) REFERENCES app_member(id),
    CONSTRAINT ck_category_active CHECK (active IN (0,1))
);
CREATE UNIQUE INDEX uk_category_owner_name ON category(NVL(member_id,0), normalized_name);

CREATE TABLE spend (
    id NUMBER(19) PRIMARY KEY,
    member_id NUMBER(19) NOT NULL,
    spend_date DATE NOT NULL,
    category_id NUMBER(19) NOT NULL,
    amount NUMBER(14,0) NOT NULL,
    memo VARCHAR2(200 CHAR),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_spend_member FOREIGN KEY (member_id) REFERENCES app_member(id),
    CONSTRAINT fk_spend_category FOREIGN KEY (category_id) REFERENCES category(id),
    CONSTRAINT ck_spend_amount CHECK (amount > 0),
    CONSTRAINT ck_spend_day CHECK (spend_date = TRUNC(spend_date))
);
CREATE INDEX ix_spend_member_date ON spend(member_id, spend_date);

CREATE TABLE daily_notification (
    id NUMBER(19) PRIMARY KEY,
    member_id NUMBER(19) NOT NULL,
    notice_date DATE NOT NULL,
    opening_amount NUMBER(19) NOT NULL,
    read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_notice_member FOREIGN KEY (member_id) REFERENCES app_member(id),
    CONSTRAINT uk_notice_member_date UNIQUE(member_id, notice_date)
);

INSERT INTO category(id, name, normalized_name, active, created_at, updated_at)
VALUES(category_seq.NEXTVAL, '식비', '식비', 1, LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO category(id, name, normalized_name, active, created_at, updated_at)
VALUES(category_seq.NEXTVAL, '교통', '교통', 1, LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO category(id, name, normalized_name, active, created_at, updated_at)
VALUES(category_seq.NEXTVAL, '생활용품', '생활용품', 1, LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO category(id, name, normalized_name, active, created_at, updated_at)
VALUES(category_seq.NEXTVAL, '문화', '문화', 1, LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO category(id, name, normalized_name, active, created_at, updated_at)
VALUES(category_seq.NEXTVAL, '기타', '기타', 1, LOCALTIMESTAMP, LOCALTIMESTAMP);
