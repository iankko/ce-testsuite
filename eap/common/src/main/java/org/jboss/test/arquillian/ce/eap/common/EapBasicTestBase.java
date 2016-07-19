package org.jboss.test.arquillian.ce.eap.common;

import java.net.URL;
import java.util.logging.Logger;

import org.jboss.arquillian.ce.cube.RouteURL;
import org.jboss.arquillian.ce.httpclient.HttpClient;
import org.jboss.arquillian.ce.httpclient.HttpClientBuilder;
import org.jboss.arquillian.ce.httpclient.HttpRequest;
import org.jboss.arquillian.ce.httpclient.HttpResponse;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.InSequence;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Jonh Wendell
 * @author Marko Luksa
 */
public class EapBasicTestBase {

    private Logger log = Logger.getLogger(getClass().getName());

    @RouteURL("eap-app")
    private URL url;

    protected URL getUrl() {
        return url;
    }

    private class Person {
        long id;
        String name;
        String email;
        String phoneNumber;

        Person(long id, String name, String email, String phoneNumber) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.phoneNumber = phoneNumber;
        }
    }

    @Before
    public void delayBetweenTests() throws InterruptedException {
        final int DELAY = 5;
        log.info(String.format("Delaying %d seconds between tests", DELAY));
        Thread.sleep(DELAY * 1000);
    }

    @Test
    @RunAsClient
    @InSequence(1)
    public void testInitialState() throws Exception {
        log.info("Trying URL " + getUrl());
        HttpClient client = HttpClientBuilder.untrustedConnectionClient();
        HttpRequest request = HttpClientBuilder.doGET(getUrl() + "/rest/members");
        HttpResponse response = client.execute(request);

        JSONParser jsonParser = new JSONParser();
        JSONArray array = (JSONArray) jsonParser.parse(response.getResponseBodyAsString());
        assertEquals(array.size(), 1);

        JSONObject remotePerson = getPerson(0);
        Person localPerson = new Person(0, "John Smith", "john.smith@mailinator.com", "2125551212");
        assertPeopleAreSame(remotePerson, localPerson);
    }

    @SuppressWarnings("unchecked")
    @Test
    @RunAsClient
    @InSequence(2)
    public void testCreatePerson() throws Exception {
        final String name = "ce-arq test for EAP";
        final String email = "cloud-enablement-feedback@redhat.com";
        final String phoneNumber = "555987654321";
        JSONObject p = new JSONObject();
        p.put("name", name);
        p.put("email", email);
        p.put("phoneNumber", phoneNumber);

        HttpClient client = HttpClientBuilder.untrustedConnectionClient();
        HttpRequest request = HttpClientBuilder.doPOST(getUrl() + "/rest/members");
        request.setHeader("Content-Type", "application/json");
        request.setEntity(p.toString());
        HttpResponse response = client.execute(request);
        assertEquals(200, response.getResponseCode());

        JSONObject remotePerson = getPerson(1);
        Person localPerson = new Person(1, name, email, phoneNumber);
        assertPeopleAreSame(remotePerson, localPerson);
    }

    private JSONObject getPerson(int id) throws Exception {
        HttpClient client = HttpClientBuilder.untrustedConnectionClient();
        HttpRequest request = HttpClientBuilder.doGET(getUrl() + "/rest/members/" + id);
        HttpResponse response = client.execute(request);

        JSONParser jsonParser = new JSONParser();
        return (JSONObject) jsonParser.parse(response.getResponseBodyAsString());
    }

    private void assertPeopleAreSame(JSONObject person1, Person person2) {
        assertEquals(person2.id, (long)person1.get("id"));
        assertEquals(person2.name, person1.get("name"));
        assertEquals(person2.email, person1.get("email"));
        assertEquals(person2.phoneNumber, person1.get("phoneNumber"));
    }
}
