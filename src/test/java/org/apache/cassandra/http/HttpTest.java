package org.apache.cassandra.http;

import static org.junit.Assert.assertEquals;

import java.net.URL;

import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpTest {
	private static final String BASE_URL = "http://localhost:8080/virgil/data/";
	private static final String COLUMN_FAMILY = "TEST_CF";
	private static final String KEYSPACE = "TEST_KEYSPACE";
	private static final String KEY = "TEST_ROW";

	private static Logger logger = LoggerFactory.getLogger(HttpTest.class);

	@BeforeClass
	public static void setup() throws Exception {
		HttpDaemon.main(null);
	}

	@AfterClass
	public static void teardown() throws Exception {
		HttpDaemon.shutdown();
	}

	@Test
	public void testHttp() throws Exception {
		HttpClient client = new HttpClient();

		// DROP KEYSPACE
		DeleteMethod delete = new DeleteMethod(BASE_URL + KEYSPACE + "/");
		this.send(client, delete, -1);

		// CREATE KEYSPACE
		PutMethod put = new PutMethod(BASE_URL + KEYSPACE + "/");
		this.send(client, put, 204);

		// CREATE COLUMN FAMILY
		put = new PutMethod(BASE_URL + KEYSPACE + "/" + COLUMN_FAMILY + "/");
		this.send(client, put, 204);

		// INSERT ROW
		put = new PutMethod(BASE_URL + KEYSPACE + "/" + COLUMN_FAMILY + "/"
				+ KEY);
		RequestEntity requestEntity = new StringRequestEntity(
				"{\"ADDR1\":\"1234 Fun St.\",\"CITY\":\"Souderton.\"}",
				"appication/json", "UTF8");
		put.setRequestEntity(requestEntity);
		this.send(client, put, 204);

		// FETCH ROW (VERIFY ROW INSERT)
		GetMethod get = new GetMethod(BASE_URL + KEYSPACE + "/" + COLUMN_FAMILY
				+ "/" + KEY);
		this.send(client, get, 200);
		String body = get.getResponseBodyAsString();
		assertEquals("{\"ADDR1\":\"1234 Fun St.\",\"CITY\":\"Souderton.\"}",
				body);
		logger.debug(body);

		// INSERT COLUMN
		put = new PutMethod(BASE_URL + KEYSPACE + "/" + COLUMN_FAMILY + "/"
				+ KEY + "/STATE/");
		requestEntity = new StringRequestEntity("CA", "appication/json", "UTF8");
		put.setRequestEntity(requestEntity);
		this.send(client, put, 204);

		// FETCH ROW (VERIFY COLUMN INSERT)
		get = new GetMethod(BASE_URL + KEYSPACE + "/" + COLUMN_FAMILY + "/"
				+ KEY);
		this.send(client, get, 200);
		body = get.getResponseBodyAsString();
		assertEquals(
				"{\"ADDR1\":\"1234 Fun St.\",\"STATE\":\"CA\",\"CITY\":\"Souderton.\"}",
				body);
		logger.debug(body);

		// FETCH COLUMN
		get = new GetMethod(BASE_URL + KEYSPACE + "/" + COLUMN_FAMILY + "/"
				+ KEY + "/CITY");
		this.send(client, get, 200);
		body = get.getResponseBodyAsString();
		assertEquals("{\"CITY\":\"Souderton.\"}", body);
		logger.debug(body);

		// DELETE COLUMN
		delete = new DeleteMethod(BASE_URL + KEYSPACE + "/" + COLUMN_FAMILY
				+ "/" + KEY + "/CITY");
		this.send(client, delete, 204);

		// VERIFY COLUMN DELETE
		get = new GetMethod(BASE_URL + KEYSPACE + "/" + COLUMN_FAMILY + "/"
				+ KEY);
		this.send(client, get, 200);
		body = get.getResponseBodyAsString();
		assertEquals("{\"ADDR1\":\"1234 Fun St.\",\"STATE\":\"CA\"}", body);
		logger.debug(body);

		// DELETE ROW
		delete = new DeleteMethod(BASE_URL + KEYSPACE + "/" + COLUMN_FAMILY
				+ "/" + KEY);
		this.send(client, delete, 204);

		// VERIFY ROW DELETE
		get = new GetMethod(BASE_URL + KEYSPACE + "/" + COLUMN_FAMILY + "/"
				+ KEY);
		this.send(client, get, 204);
		body = get.getResponseBodyAsString();
		assertEquals(null, body);
		logger.debug(body);

		// CLEANUP : DROP COLUMN FAMILY
		delete = new DeleteMethod(BASE_URL + KEYSPACE + "/" + COLUMN_FAMILY
				+ "/");
		this.send(client, delete, -1);

		// CLEANUP : DROP KEYSPACE
		delete = new DeleteMethod(BASE_URL + KEYSPACE + "/");
		this.send(client, delete, -1);
	}

	private void send(HttpClient client, HttpMethod method, int expect)
			throws Exception {
		try {
			int result = client.executeMethod(method);
			String body = method.getResponseBodyAsString();
			logger.debug(body);
			if (expect > 0)
				assertEquals(expect, result);
		} finally {
			method.releaseConnection();
		}
	}
}
