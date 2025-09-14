-- liquibase formatted sql
-- changeset Abu:create_cast
CREATE CAST (varchar AS card_status) WITH INOUT AS IMPLICIT;
CREATE CAST (varchar AS transaction_status) WITH INOUT AS IMPLICIT;
CREATE CAST (varchar AS card_application_status) WITH INOUT AS IMPLICIT;