--
-- PostgreSQL database dump
--

-- Dumped from database version 16.3 (Ubuntu 16.3-0ubuntu0.24.04.1)
-- Dumped by pg_dump version 16.3 (Ubuntu 16.3-0ubuntu0.24.04.1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: public; Type: SCHEMA; Schema: -; Owner: -
--

-- *not* creating schema, since initdb creates it


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: adp; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.adp (
    co_code character varying(3) NOT NULL,
    batch_id integer NOT NULL,
    file_no character varying(4) NOT NULL,
    reg_hours real NOT NULL,
    hours_3_code_e04 character varying(3) NOT NULL,
    hours_3_amount_e04 real NOT NULL,
    hours_3_code_e20 character varying(3) NOT NULL,
    hours_3_amount_e20 real,
    earnings_3_code_e16 character varying(3) NOT NULL,
    vac_hours real NOT NULL,
    earnings_3_amount_e16 real NOT NULL,
    name text NOT NULL,
    wage real,
    earnings_3_code_e30 character varying(3) NOT NULL,
    earnings_3_amount_e30 real NOT NULL,
    earnings_3_code_e31 character varying(3) NOT NULL,
    earnings_3_amount_e31 real NOT NULL,
    earnings_3_code_e12 character varying(3) NOT NULL,
    earnings_3_amount_e12 real NOT NULL,
    blank text,
    roe text,
    blank2 text
);


--
-- Name: amapi_device; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.amapi_device (
    device_id integer NOT NULL,
    device_name character varying(43) NOT NULL,
    account_id character varying(100),
    policy_id character varying(101) NOT NULL,
    additional_data character varying(1024) NOT NULL,
    token_data character varying(100),
    enterprise_id character varying(100),
    user_id integer,
    serial character varying(50)
);


--
-- Name: amapi_device_device_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.amapi_device_device_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: amapi_device_device_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.amapi_device_device_id_seq OWNED BY public.amapi_device.device_id;


--
-- Name: amapi_device_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.amapi_device_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: amapi_token; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.amapi_token (
    token_id integer NOT NULL,
    tok_name character varying(43),
    tok_value character varying(20),
    account_id character varying(100) NOT NULL,
    one_time_only boolean NOT NULL,
    duration double precision,
    allow_personal_useage boolean NOT NULL,
    policy_id character varying(101) NOT NULL,
    additional_data character varying(1024) NOT NULL,
    response_code text,
    expiration_timestamp timestamp with time zone
);


--
-- Name: amapi_token_token_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.amapi_token_token_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: amapi_token_token_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.amapi_token_token_id_seq OWNED BY public.amapi_token.token_id;


--
-- Name: banked; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.banked (
    post_date date DEFAULT now() NOT NULL,
    pay_period character varying(10) NOT NULL,
    user_id integer NOT NULL,
    type character varying(30) NOT NULL,
    hours real NOT NULL,
    phys_worked real,
    overtime real,
    sick real,
    stat_pay real,
    stat_bonus real,
    nrsi_bonus real,
    unbanked real,
    vac_rt real,
    vac real,
    std real
);


--
-- Name: proj_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.proj_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: cxlist; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cxlist (
    job_id character varying(20) NOT NULL,
    job_name character varying(255) NOT NULL,
    proj_id integer DEFAULT nextval('public.proj_id_seq'::regclass) NOT NULL,
    rep_id integer,
    province character(2) NOT NULL,
    ts_active boolean,
    qb_active boolean DEFAULT false NOT NULL
);


--
-- Name: tscell_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tscell_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: master2; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.master2 (
    period character varying(10) NOT NULL,
    date date NOT NULL,
    duration double precision NOT NULL,
    note text NOT NULL,
    old_id character varying(7) NOT NULL,
    ot character varying(255) NOT NULL,
    user_id integer NOT NULL,
    id integer DEFAULT nextval('public.tscell_id_seq'::regclass) NOT NULL,
    proj_id integer NOT NULL,
    txn_id character varying(36)
);


--
-- Name: period_status; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.period_status (
    pay_period character varying(10) NOT NULL,
    invoicing_status character varying(15) NOT NULL,
    payroll_status character varying(15) NOT NULL
);


--
-- Name: province; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.province (
    prov character(2) NOT NULL,
    prov_name character varying(25) NOT NULL
);


--
-- Name: qb_vendor; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.qb_vendor (
    v_list_id character varying(30) NOT NULL,
    v_name character varying(41) NOT NULL,
    v_type character varying(31),
    v_active boolean NOT NULL,
    v_nameoncheck character varying(41),
    v_email character varying(41)
);


--
-- Name: qbitem; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.qbitem (
    i_list_id character varying(30) NOT NULL,
    name character varying(31) NOT NULL,
    sales_tax character varying(10),
    price real,
    account character varying(50),
    i_desc character varying(100),
    category character varying(20) NOT NULL,
    i_active boolean NOT NULL,
    i_p_list_id character varying(30)
);


--
-- Name: rep_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.rep_id_seq
    AS integer
    START WITH 3
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: rep; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.rep (
    rep_id integer DEFAULT nextval('public.rep_id_seq'::regclass) NOT NULL,
    rep_name character varying(5) NOT NULL,
    rep_qb_list_id character varying(25) NOT NULL,
    rep_user_id integer NOT NULL
);


--
-- Name: ss_project2; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ss_project2 (
    job_id character varying(10),
    job_name character varying(255),
    ss_active character varying(35),
    proj_id integer
);


--
-- Name: ss_staff; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ss_staff (
    qb_username character varying(40) NOT NULL,
    email character varying(30) NOT NULL,
    contact character varying(40) NOT NULL,
    pref_num character varying(20),
    sms character varying(20),
    supervisor character varying(50),
    user_id integer,
    sv_user_id integer
);


--
-- Name: staff_leave; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.staff_leave (
    user_id integer NOT NULL,
    on_leave boolean NOT NULL
);


--
-- Name: stat_days; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.stat_days (
    holiday date NOT NULL,
    holiday_name character varying(20) NOT NULL,
    on_legal boolean DEFAULT true NOT NULL,
    ab_legal boolean DEFAULT true NOT NULL,
    sk_legal boolean DEFAULT true NOT NULL
);


--
-- Name: tsuser; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tsuser (
    user_id integer NOT NULL,
    username character varying(25),
    vac_rate real DEFAULT 0.04 NOT NULL,
    active boolean DEFAULT false NOT NULL,
    qb_email character varying(100) NOT NULL,
    salaried boolean DEFAULT false NOT NULL,
    prov character(2) DEFAULT 'ON'::bpchar NOT NULL,
    qb_emp_list_id character varying(25),
    it_username character varying(100) NOT NULL,
    sv_user_id integer,
    pref_num character varying(15),
    management boolean DEFAULT false NOT NULL,
    hours_limit real DEFAULT 40 NOT NULL,
    personal_time integer DEFAULT 24 NOT NULL,
    qb_emp_acct_no character varying(99)
);


--
-- Name: users_user_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.users_user_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: users_user_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.users_user_id_seq OWNED BY public.tsuser.user_id;


--
-- Name: amapi_device device_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.amapi_device ALTER COLUMN device_id SET DEFAULT nextval('public.amapi_device_device_id_seq'::regclass);


--
-- Name: amapi_token token_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.amapi_token ALTER COLUMN token_id SET DEFAULT nextval('public.amapi_token_token_id_seq'::regclass);


--
-- Name: tsuser user_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tsuser ALTER COLUMN user_id SET DEFAULT nextval('public.users_user_id_seq'::regclass);


--
-- Name: adp adp_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.adp
    ADD CONSTRAINT adp_pkey PRIMARY KEY (file_no);


--
-- Name: amapi_device amapi_device_device_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.amapi_device
    ADD CONSTRAINT amapi_device_device_name_key UNIQUE (device_name);


--
-- Name: amapi_device amapi_device_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.amapi_device
    ADD CONSTRAINT amapi_device_pkey PRIMARY KEY (device_id);


--
-- Name: amapi_token amapi_token_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.amapi_token
    ADD CONSTRAINT amapi_token_pkey PRIMARY KEY (token_id);


--
-- Name: tsuser emp_acct_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tsuser
    ADD CONSTRAINT emp_acct_no_key UNIQUE (qb_emp_acct_no);


--
-- Name: cxlist proj_id_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cxlist
    ADD CONSTRAINT proj_id_pkey PRIMARY KEY (proj_id);


--
-- Name: province province_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.province
    ADD CONSTRAINT province_pkey PRIMARY KEY (prov);


--
-- Name: province province_prov_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.province
    ADD CONSTRAINT province_prov_name_key UNIQUE (prov_name);


--
-- Name: tsuser qb_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tsuser
    ADD CONSTRAINT qb_email_key UNIQUE (qb_email);


--
-- Name: qbitem qb_item_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.qbitem
    ADD CONSTRAINT qb_item_name_key UNIQUE (name);


--
-- Name: qbitem qb_list_item_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.qbitem
    ADD CONSTRAINT qb_list_item_key UNIQUE (i_list_id);


--
-- Name: qb_vendor qb_vendor_v_list_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.qb_vendor
    ADD CONSTRAINT qb_vendor_v_list_id_key UNIQUE (v_list_id);


--
-- Name: qb_vendor qb_vendor_v_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.qb_vendor
    ADD CONSTRAINT qb_vendor_v_name_key UNIQUE (v_name);


--
-- Name: rep rep_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rep
    ADD CONSTRAINT rep_name_key UNIQUE (rep_name);


--
-- Name: rep rep_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rep
    ADD CONSTRAINT rep_pkey PRIMARY KEY (rep_id);


--
-- Name: master2 tscell_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.master2
    ADD CONSTRAINT tscell_pkey PRIMARY KEY (id);


--
-- Name: tsuser tsuser_it_username_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tsuser
    ADD CONSTRAINT tsuser_it_username_key UNIQUE (it_username);


--
-- Name: tsuser tsuser_qb_emp_list_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tsuser
    ADD CONSTRAINT tsuser_qb_emp_list_id_key UNIQUE (qb_emp_list_id);


--
-- Name: tsuser username_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tsuser
    ADD CONSTRAINT username_key UNIQUE (username);


--
-- Name: tsuser users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tsuser
    ADD CONSTRAINT users_pkey PRIMARY KEY (user_id);


--
-- Name: cxlist_job_id_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX cxlist_job_id_key ON public.cxlist USING btree (job_id);


--
-- Name: master2 proj_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.master2
    ADD CONSTRAINT proj_id_fk FOREIGN KEY (proj_id) REFERENCES public.cxlist(proj_id);


--
-- Name: tsuser prov_code_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tsuser
    ADD CONSTRAINT prov_code_fk FOREIGN KEY (prov) REFERENCES public.province(prov);


--
-- Name: cxlist rep_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cxlist
    ADD CONSTRAINT rep_id_fk FOREIGN KEY (rep_id) REFERENCES public.rep(rep_id);


--
-- Name: staff_leave staff_leave_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.staff_leave
    ADD CONSTRAINT staff_leave_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.tsuser(user_id);


--
-- Name: master2 user_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.master2
    ADD CONSTRAINT user_id_fk FOREIGN KEY (user_id) REFERENCES public.tsuser(user_id);


--
-- Name: banked user_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.banked
    ADD CONSTRAINT user_id_fk FOREIGN KEY (user_id) REFERENCES public.tsuser(user_id);


--
-- Name: rep user_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rep
    ADD CONSTRAINT user_id_fk FOREIGN KEY (rep_user_id) REFERENCES public.tsuser(user_id);


--
-- Name: tsuser user_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tsuser
    ADD CONSTRAINT user_id_fk FOREIGN KEY (sv_user_id) REFERENCES public.tsuser(user_id);


--
-- PostgreSQL database dump complete
--

