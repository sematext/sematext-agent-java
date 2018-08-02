namespace java com.sematext.spm.client.tracing.thrift

// call tag
enum TCallTag {
   // regular call, don't has any annotations
   REGULAR = 0,
   // sql query, call should have TSQLAnnotation
   SQL_QUERY = 1,
   // external call, not used
   EXTERNAL = 2,
   // jpa, should have TJPAAnnotation
   JPA = 3,
   // jsp, should have TJSPAnnotation
   JSP = 4,
   // http request, should have THTTPRequestAnnotation
   HTTP_REQUEST = 5,
   // elasticsearch request, should have TESAnnotation
   ES = 6,
   // solr request, should have TSolrAnnotation
   SOLR = 7
}

// transaction type
enum TTransactionType {
   WEB = 0,
   BACKGROUND = 1
}

// failure type
enum TFailureType {
   EXCEPTION = 0,
   HTTP_RESPONSE = 1,
   CUSTOM = 2
}

// solr request type
enum TSolrRequestType {
   SCHEMA = 0,
   UPDATE = 1,
   COLLECTION_ADMIN = 2,
   CORE_ADMIN = 3,
   ANALYSIS = 4,
   QUERY = 5,
   OTHER = 6
}

enum TSqlStatementOperation {
   SELECT = 0,
   DELETE = 1,
   UPDATE = 2,
   INSERT = 3,
   OTHER = 4
}

// sql annotation
struct TSQLAnnotation {
   // not supported now, should be 'query'
   1: required string type;
   // executed sql
   2: optional string sql;
   // results count
   3: optional i32 count;
   // database url
   4: optional string url;
   // operation type
   5: optional TSqlStatementOperation operation;
   // table name
   6: optional string table;
}

// jpa annotation
struct TJPAAnnotation {
   // jpa call type: either 'query' or 'find'
   1: required string type;
   // jpa query
   2: optional string query;
   // queried object
   3: optional string object;
   // results count
   4: optional i32 count;
}

// jsp annotation
struct TJSPAnnotation {
   // path to rendered jsp
   1: optional string path;
}

// http request annotation
struct THTTPRequestAnnotation {
   // request url
   1: optional string url;
   // request method
   2: optional string method;
   // response code
   3: optional i32 responseCode;
}

// inet address
struct TInetAddress {
   1: required string host;
   2: required i32 port;
}

// elasticsearch operation type
enum TESOperationType {
   INDEX = 0;
   DELETE = 1;
   SEARCH = 2;
   GET = 3;
   UPDATE = 4;
}

// elasticsearch request type
enum TESRequestType {
    INDEX_BULK = 0;
    UPDATE_BULK = 1;
    DELETE_BULK = 2;
    BULK = 3;
    INDEX = 4;
    DELETE = 5;
    SEARCH = 6;
    GET = 7;
    UPDATE = 8;
}

/**
* ElasticSearch action. Represents one item from bulk request.
**/
struct TESAction {
   //deprecated
   1: optional string opType;
   // index
   2: optional string index;
   // type
   3: optional string type;
   // query
   4: optional string query;
   //deprecated - use Call.failed instead
   5: optional bool succeed;
   //todo: make required after remove opType
   // operation type
   6: optional TESOperationType operationType;
   // Actions count. Used for actions grouping where there are one more action with same operationType,
   7: optional i32 count;
}

// elasticsearch annotation
struct TESAnnotation {
   // elasticsearch actions (multiple, because can be performed in bulk)
   1: required list<TESAction> actions;
   //deprecated - use addresses instead
   2: optional string url;
   //todo: make required after remove 'url'
   // elasticsearch addresses
   3: optional list<TInetAddress> addresses;
   // index
   4: optional string index;
   // request type
   5: optional TESRequestType requestType;
}

// solr annotation
struct TSolrAnnotation {
   // collection
   1: optional string collection;
   // is request succeeed
   2: required bool succeed;
   // solr url
   3: required string url;
   // response status
   4: required i16 responseStatus;
   // request type
   5: required TSolrRequestType requestType;
   // request parameters
   6: required map<string, string> params;
}

// endpoint
struct TEndpoint {
   // resolution (ipv4 or ipv6)
   1: required string address;
   // hostname
   2: required string hostname;
}

// wen transaction summary
struct TWebTransactionSummary {
   // request uri part
   1: optional string request;
   // query string
   2: optional string queryString;
   // response code
   3: optional i32 responseCode;
   // request method
   4: optional string requestMethod;
}

/**
* Transaction call.
**/
struct TCall {
   // callId
   1: required i64 callId,
   // parentCallId
   2: required i64 parentCallId;
   // call depth
   4: required i32 level;
   // start timestamp in ms
   6: required i64 startTimestamp;
   // end timestamp in ms
   7: required i64 endTimestamp;
   // call duration in ms
   8: required i64 duration;
   // self call duration in ms (duration - sum of duration for children calls)
   9: required i64 selfDuration;
   // call signature
   11: required string signature;
   // is call failed
   14: optional bool failed;
   // is this call for external service?
   15: optional bool external;
   // call tag
   19: required TCallTag tag = TCallTag.REGULAR;
   // is call transactio entry point
   21: required bool entryPoint;
   /**
   * Cross application call credentials. Current call is caller, credentials are for callee. Used to identify root call
    * for callee transaction
   **/
   // cross app call token
   22: optional string crossAppToken;
   // cross app call id
   23: optional i64 crossAppCallId;
   // cross app call parent call id
   24: optional i64 crossAppParentCallId;
   // cross app call duration in ms
   25: optional i64 crossAppDuration;
   // is cross app event sampled
   27: optional bool crossAppSampled;
   /**
   * Thrift encoded annotation.
   * Depends on tag (see TCallTag)
   **/
   28: optional binary annotation;
   // custom call parameters
   29: optional map<string, string> parameters;
}

/**
* Partial Transaction. Idea is, that transaction itself can have multiple threads. Each traced thread is treated as PartialTransaction.
*
* Note on generation callId/parentId/traceId - this triple should be unique across all transactions and generated on the client. Since it is tricky
* give strong guarantees, secure random generator can be used.
**/
struct TPartialTransaction {
   // root call callId
   1: required i64 callId;
   // root call parentCallId
   2: required i64 parentCallId;
   // root call traceId
   3: required i64 traceId;
   // request name
   4: optional string request;
   // start ts in ms
   5: required i64 startTimestamp;
   // end ts in ms
   6: required i64 endTimestamp;
   // tansaction duration in ms
   7: required i64 duration;
   // application token
   8: required string token;
   // is transaction failed
   9: optional bool failed;
   // Is this transaction an entry point? Entry point is main thread.
   10: required bool entryPoint;
   // transaction type
   11: required TTransactionType transactionType = TTransactionType.WEB;
   // thrift encoded transaction summary (depends on transaction type - for web transaction it could be TWebTransactionSummary instance)
   12: optional binary transactionSummary;
   // endpoint
   13: optional TEndpoint endpoint;
   // is this transaction synchronous? should be false if started from other thread / callback
   14: optional bool asynchronous;
   // list of calls for this transaction
   15: required list<TCall> calls;
   // if transaction is falied - failure type
   16: optional TFailureType failureType;
   // stacktrace (if transaction is failed with TFailureType.EXCEPTION)
   18: optional binary exceptionStackTrace;
   // custom transaction parameters
   19: optional map<string, string> parameters;
}

// Transaction Error
struct TTracingError {
   1: required string token;
   2: optional i64 traceId;
   3: optional i64 parentCallId;
   4: optional i64 callId;
   5: required i64 timestamp;
   6: required bool sampled;
   7: required map<string, string> parameters;
}

enum TTracingEventType {
   PARTIAL_TRANSACTION = 0,
   TRACING_ERROR = 1
}

// tracing event
struct TTracingEvent {
   1: required TTracingEventType eventType;
   2: optional TPartialTransaction partialTransaction;
   3: optional TTracingError tracingError;
}