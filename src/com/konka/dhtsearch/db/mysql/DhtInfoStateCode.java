package com.konka.dhtsearch.db.mysql;

public interface DhtInfoStateCode {
	int NO_DOWNLOAD = 100;
	int DOWNLOAD_FAILED = 401;
	int DOWNLOAD_SUCCESS_BUT_PARSING_FAILED = 204;
	int DOWNLOADSUCCESS_AND_PARSING_SUCCESS = 200;
}
