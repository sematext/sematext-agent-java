namespace java com.sematext.spm.client.monitor.thrift

enum TCommandType {
    // TCommand.request should be set to thrift encoded TProfileRequest
    // TCommandResponse.response should be set to thrift encoded TProfileResponse
    PROFILE = 0,
    PING = 1,
    CANCEL = 2,
    GET_INSTRUMENTED_METHODS = 3,
    ENABLE_TRACING = 4,
    DISABLE_TRACING = 5,
    IS_TRACING_ENABLED = 6,
    UPDATE_INSTRUMENTATION_SETTINGS = 7
}

enum TCommandResponseStatus {
    SUCCESS = 0,
    FAILURE = 1
}

struct TCommand {
    0: required TCommandType type;
    // unique command id
    1: required i64 id;
    // thrift binary encoded request, depends on TCommandType
    2: optional binary request;
}

struct TCommandResponse {
    0: required TCommandResponseStatus status;
    // command id sent from the server (using TCommand)
    1: required i64 id;
    // thrift binary encoded response, depends on TCommandType
    2: optional binary response;
    3: optional string failureReason;
}

struct TCancelRequest {
    // command id sent from the server
    0: required i64 id;
}

struct TProfileRequest {
    0: required i32 durationMillis;
    1: required i32 periodMillis;
    // do need to automatically exclude agent methods from nbeing recorded
    2: required bool excludeAgentMethods;
}

struct TGetInstrumentedMethodsResponse {
    0: required list<string> methodSignatures;
}

struct TIsTracingEnabledResponse {
    0: required bool enabled;
}

struct TInstrumentedMethodState {
    // should be instrumented
    1: required bool enabled;
    // should be acted as entry point
    2: required bool entryPoint;
}

struct TUpdateInstrumentationSettings {
    // key - method/constructor signature, value - state of method/pointcut instrumentation
    //
    // [return-type] [type]#[method]([parameter-type-1],[parameter-type-2]..)
    // int java.util.ArrayList#indexOf(java.lang.Object)
    // java.lang.Object org.commons.utils.List#get(int)
    // java.lang.Object[] java.util.ArrayList#toArray()
    //
    // constructor:
    // [type]([parameter-type-1],[parameter-type-2],...)
    // java.util.ArrayList(int)
    // java.util.ArrayList(java.util.Collection)
    //
    0: required map<string, TInstrumentedMethodState> states;
}

/**
* Call tree node
**/
struct TCallNode {
    0: optional string declaringClass;
    1: optional string methodName;
    2: optional string fileName;
    3: optional i32 lineNumber;
    // how much time this method was sampled by profiler
    4: optional i32 samples;
    // total time spent in this method (milliseconds)
    5: optional i64 time;
    // cpu time spent in this method (milliseconds)
    6: optional i64 cpuTime;
    // user cpu time spent in this method (milliseconds)
    7: optional i64 userCPUTime;
    // node id
    8: required i32 id;
    // id of node children
    9: required list<i32> children;
}

/**
* CallTree representation
**/
struct TCallTree {
    // id for root node
    0: required i32 rootNodeId;
    // call tree nodes
    1: required list<TCallNode> nodes;
}

// profile
struct TAllThreadsProfileSnapshot {
    0: required TCallTree tree;
    /**
    * Total wall clock time spent by application threads.
    * It shouldn't be confused with total time spent by profiler.
    * (milliseconds)
    **/
    1: optional i32 time;
    // cpu time spent by application (milliseconds)
    2: optional i32 cpuTime;
    // user cpu time spent by application (milliseconds)
    3: optional i32 userCPUTime;
    // total thread wait time (milliseconds)
    4: optional i32 waitedTime;
    // total thread blocked time (milliseconds)
    5: optional i32 blockedTime;
    // total samples made by profiler
    6: optional i32 samples;
    // time which spent application in garbage collector during profiling (milliseconds)
    7: optional i32 gcTime;
    // is cpu time measuring supported?
    8: optional bool cpuTimeSupported;
}

struct TProfileResponse {
    0: required TAllThreadsProfileSnapshot allThreadsProfileSnapshot;
}
