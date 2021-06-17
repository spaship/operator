package io.spaship.operator.utility;

public interface EventAttribute {

    //CRO ops and Preview event attributes
    String CR_NAME = "cr_name:";
    String NAMESPACE = "namespace:";
    String MESSAGE = "message:";
    String CODE="CODE:";
    enum EventCode{
        WEBSITE_CREATE,
        WEBSITE_UPDATE,
        WEBSITE_DELETE,
        PREVIEW_CREATE,
        PREVIEW_UPDATE,
        PREVIEW_DELETE,
        RELEASE_DEPLOY,
        RELEASE_DELETE;
    }


}
