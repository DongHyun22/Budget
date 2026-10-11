CREATE SEQUENCE member_notification_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE member_notification (
    id NUMBER(19) PRIMARY KEY,
    member_id NUMBER(19) NOT NULL,
    notice_date DATE NOT NULL,
    remaining_amount NUMBER(19),
    created_at TIMESTAMP NOT NULL,
    read_at TIMESTAMP,
    CONSTRAINT fk_member_notice_member FOREIGN KEY (member_id) REFERENCES app_member(id),
    CONSTRAINT uk_member_notice_date UNIQUE (member_id, notice_date)
);
