


CREATE TABLE WEB_DATA (
    id INT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255),
    url VARCHAR(255) UNIQUE, 
    description VARCHAR(1024),
	access_frequency INT DEFAULT 0, 
    sponsorship_level INT DEFAULT 0
);

CREATE TABLE KWIC_INDEX (
	page_id INT,
	keyword VARCHAR(50),
    shift_amount INT,
    PRIMARY KEY (page_id, keyword, shift_amount),
    FOREIGN KEY (page_id) REFERENCES WEB_DATA(id) ON DELETE CASCADE
);

CREATE INDEX idx_keyword
ON KWIC_INDEX (keyword);

CREATE INDEX idx_kwic_keyword_page
ON KWIC_INDEX (keyword, page_id);

CREATE INDEX idx_kwic_page_keyword
ON KWIC_INDEX (page_id, keyword);
