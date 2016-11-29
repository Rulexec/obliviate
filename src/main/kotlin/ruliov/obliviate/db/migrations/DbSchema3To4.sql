CREATE TABLE emails
(
  email character varying(255) NOT NULL,
  CONSTRAINT emails_pkey PRIMARY KEY (email)
)
WITH (
  OIDS=FALSE
);