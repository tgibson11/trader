USE trd;

CREATE TABLE parameter
  (parameter_cd    VARCHAR(100)  NOT NULL PRIMARY KEY,
   parameter_value VARCHAR(100) NOT NULL)
  ENGINE=InnoDB;

CREATE TABLE account
  (account_id VARCHAR(10) NOT NULL PRIMARY KEY)
  ENGINE=InnoDB;

CREATE TABLE exchange
  (exchange_id INTEGER     NOT NULL AUTO_INCREMENT PRIMARY KEY,
   exchange_cd VARCHAR(10) NOT NULL)
  ENGINE=InnoDB;

CREATE TABLE underlying
  (underlying_id INTEGER     NOT NULL AUTO_INCREMENT PRIMARY KEY,
   description   VARCHAR(20) NOT NULL)
  ENGINE=InnoDB;
   
CREATE TABLE correlation
  (symbol            VARCHAR(3) NOT NULL, 
   correlated_symbol VARCHAR(3) NOT NULL,
   correlation_type  VARCHAR(4) NOT NULL,
   CONSTRAINT correlation_pk  PRIMARY KEY (symbol, correlated_symbol),
   CONSTRAINT correlation_fk1 FOREIGN KEY (symbol)            REFERENCES contract (symbol),
   CONSTRAINT correlation_fk2 FOREIGN KEY (correlated_symbol) REFERENCES contract (symbol))
  ENGINE=InnoDB;
   
CREATE TABLE contract
  (symbol        VARCHAR(3)    NOT NULL PRIMARY KEY,
   exchange_id   INTEGER       NOT NULL,    
   underlying_id INTEGER       NOT NULL,
   multiplier    NUMERIC(10,1) NOT NULL,
   tick_size     NUMERIC(10,6) NOT NULL,
   cents_flag    NUMERIC(1)    NOT NULL,
   active_flag   NUMERIC(1)    NOT NULL,
   expiry        VARCHAR(6),
   atr           NUMERIC(10,6),
   entry_high    NUMERIC(10,6),
   entry_low     NUMERIC(10,6),
   exit_high     NUMERIC(10,6),
   exit_low      NUMERIC(10,6),
   open_interest_url VARCHAR(1000),
   CONSTRAINT contract_fk1 FOREIGN KEY (exchange_id)   REFERENCES exchange (exchange_id),
   CONSTRAINT contract_fk2 FOREIGN KEY (underlying_id) REFERENCES underlying (underlying_id))
  ENGINE=InnoDB;

CREATE TABLE account_contract
  (account_id VARCHAR(10) NOT NULL,
   symbol     VARCHAR(3)  NOT NULL,
   unit_size  INTEGER     NOT NULL,
   CONSTRAINT account_contract_pk  PRIMARY KEY (account_id, symbol),
   CONSTRAINT account_contract_fk1 FOREIGN KEY (account_id) REFERENCES account (account_id),
   CONSTRAINT account_contract_fk2 FOREIGN KEY (symbol)     REFERENCES contract (symbol))
  ENGINE=InnoDB;

CREATE TABLE account_hist
  (account_id  VARCHAR(10)   NOT NULL,
   date        DATETIME      NOT NULL, 
   deposits    NUMERIC(10,2) NOT NULL DEFAULT 0,
   withdrawals NUMERIC(10,2) NOT NULL DEFAULT 0,
   value       NUMERIC(10,2) NOT NULL,
   CONSTRAINT account_hist_pk  PRIMARY KEY (account_id, date),
   CONSTRAINT account_hist_fk1 FOREIGN KEY (account_id) REFERENCES account (account_id))
  ENGINE=InnoDB;

CREATE TABLE trade
  (trade_id    INTEGER       NOT NULL AUTO_INCREMENT PRIMARY KEY,
   account_id  VARCHAR(10)   NOT NULL, 
   entry_dt    DATE          NOT NULL, 
   symbol      VARCHAR(3)    NOT NULL, 
   quantity    INTEGER       NOT NULL, 
   entry_price NUMERIC(10,6) NOT NULL,
   stop_price  NUMERIC(10,6) NOT NULL,
   exit_dt     DATE, 
   exit_price  NUMERIC(10,6), 
   CONSTRAINT trade_fk1 FOREIGN KEY (account_id) REFERENCES account (account_id),
   CONSTRAINT trade_fk2 FOREIGN KEY (symbol)     REFERENCES contract (symbol))
  ENGINE=InnoDB;
   
CREATE TABLE contract_month
  (symbol   VARCHAR(3)  NOT NULL,
   month    VARCHAR(2)  NOT NULL,
   CONSTRAINT contract_month_pk  PRIMARY KEY (symbol, month),
   CONSTRAINT contract_month_fk1 FOREIGN KEY (symbol) REFERENCES contract (symbol))
  ENGINE=InnoDB;
  
CREATE TABLE execution (
	execution_id	INTEGER			NOT NULL PRIMARY KEY,
	execution_dt	DATETIME		NOT NULL,
	account_id		VARCHAR(10)		NOT NULL,
	order_id		INTEGER 		NOT NULL,
	symbol			VARCHAR(3)		NOT NULL,
	action          VARCHAR(3)  	NOT NULL,
	quantity        INTEGER			NOT NULL,
	price			NUMERIC(10,6)	NOT NULL,
	order_price		NUMERIC(10,6),
	CONSTRAINT execution_fk1	FOREIGN KEY (account_id)	REFERENCES account (account_id),
	CONSTRAINT execution_fk2	FOREIGN KEY (symbol)		REFERENCES contract (symbol))
	ENGINE=InnoDB;
	