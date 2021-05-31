package io.spaship.operator.gitapi;

import io.quarkus.test.junit.QuarkusTest;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.io.IOException;

@QuarkusTest
class GitlabApiTest {

    static MockWebServer gitApiMock;
    @Inject
    GitlabApi gitlabApi;

    @BeforeAll
    static void beforeAll() throws IOException {
        gitApiMock = new MockWebServer();
        gitApiMock.start(9001);
    }

    @AfterAll
    static void afterAll() throws IOException {
        gitApiMock.shutdown();
    }

    @Test
    void commentMergeRequest() throws InterruptedException {
        gitApiMock.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                return new MockResponse().setResponseCode(200).setBody("{\"test1\":\"test2\"}");
            }
        });

        try {
            gitlabApi.commentMergeRequest("http://localhost:9001/", "555", "1", "access-token", "comment with space")
                    .onFailure(Assertions::fail)
                    .onSuccess(event -> Assertions.assertEquals("test2", event.getString("test1")))
                    .wait();
        } catch (Exception e) {
            // wait completed
        }
        RecordedRequest request = gitApiMock.takeRequest();
        Assertions.assertEquals(String.format(GitlabApi.MR_COMMENT_API, "555", "1", "comment+with+space"), request.getPath());
        Assertions.assertEquals("access-token", request.getHeader("PRIVATE-TOKEN"));

    }

}