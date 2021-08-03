package io.spaship.operator.utility;

public interface EventAttribute {

    //CRO ops and Preview event attributes
    String CR_NAME = "cr_name~";
    String NAMESPACE = "namespace~";
    String MESSAGE = "message~";
    String CODE="CODE~";
    String TRACE_ID="traceId~";
    String TIMESTAMP="timestamp~";
    String ERROR="error~";
    String ENVIRONMENT="target-env~";
    enum EventCode{
        WEBSITE_CREATE_INIT,
        WEBSITE_CREATE_OR_UPDATE_INIT,
        WEBSITE_REFRESH_COMPONENT_INIT,
        WEBSITE_REFRESH_COMPONENT,
        WEBSITE_REFRESH_COMPONENT_FAILED,
        WEBSITE_INIT_FAILED,
        WEBSITE_CREATE,
        WEBSITE_UPDATE,
        WEBSITE_DELETE_INIT,
        WEBSITE_DELETED,
        PREVIEW_CREATE,
        PREVIEW_UPDATE,
        RELEASE_DEPLOY,
        RELEASE_DELETE;
    }


}
