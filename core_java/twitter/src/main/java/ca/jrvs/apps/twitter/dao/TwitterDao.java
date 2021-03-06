package ca.jrvs.apps.twitter.dao;

import ca.jrvs.apps.twitter.model.Tweet;
import ca.jrvs.apps.twitter.util.JsonUtil;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Repository

public class TwitterDao implements CrdDao<Tweet, String> {

    //URI constants
    private static final String API_BASE_URI = "https://api.twitter.com";
    private static final String POST_PATH = "/1.1/statuses/update.json";
    private static final String SHOW_PATH = "/1.1/statuses/show.json";
    private static final String DELETE_PATH = "/1.1/statuses/destroy/";
    private static final String USER_PATH = "/1.1/statuses/user_timeline.json";
    //URI symbols
    private static final String QUERY_SYM = "?";
    private static final String AMPERSAND = "&";
    private static final String EQUAL = "=";

    //Response code
    private static final int HTTP_OK = 200;

    private HttpHelper httpHelper;

    final Logger logger = LoggerFactory.getLogger(TwitterDao.class);

    @Autowired
    public TwitterDao(HttpHelper httpHelper) {this.httpHelper = httpHelper;}

    /**
     * Create an entity(Tweet) to the underlying storage
     *
     * @param tweet entity that to be created
     * @return created entity
     */
    @Override
    public Tweet create(Tweet tweet) {
        //Construct URI
        URI uri;
        try {
            uri = getPostUri(tweet);
        } catch (URISyntaxException | UnsupportedEncodingException e){
            throw new IllegalArgumentException("Invalid tweet input", e);
        }

        // Execute HTTP Request
        HttpResponse response = httpHelper.httpPost(uri);

        //Validate response and deserialize response to Tweet object
        return parseResponseBody(response, HTTP_OK);
    }

    /**
     * Construct a twitter POST URI
     *
     * @param tweet
     * @throws URISyntaxException when URI is invalid
     * @throws UnsupportedEncodingException when failed to encode text
     * @return
     */
    private URI getPostUri(Tweet tweet) throws URISyntaxException, UnsupportedEncodingException{
        String text = tweet.getText();
        Double longitude = tweet.getCoordinates().getCoordinates().get(0);
        Double latitude = tweet.getCoordinates().getCoordinates().get(1);

        StringBuilder sb = new StringBuilder();
        sb.append(API_BASE_URI)
                .append(POST_PATH)
                .append(QUERY_SYM);

        appendQueryParam(sb, "status", URLEncoder.encode(text, StandardCharsets.UTF_8.name()), true);
        appendQueryParam(sb, "long", longitude.toString(), false);
        appendQueryParam(sb, "lat", latitude.toString(), false);

        return new URI(sb.toString());
    }

    private void appendQueryParam(StringBuilder sb, String key, String value, boolean firstParam) {
        if (!firstParam) {
            sb.append(AMPERSAND);
        }
        sb.append(key)
                .append(EQUAL)
                .append(value);
    }

    /**
     * Find an entity(Tweet) by its id
     *
     * @param s entity id
     * @return Tweet entity
     */

    @Override
    public Tweet findById(String s) throws URISyntaxException {
        //Construct URI

        StringBuilder sb = new StringBuilder();
        sb.append(API_BASE_URI)
                .append(SHOW_PATH)
                .append(QUERY_SYM)
                .append("id=")
                .append(s);

        URI uri = new URI(sb.toString());

        // Execute HTTP Request
        HttpResponse response = httpHelper.httpGet(uri);

        //Validate response and deser response to Tweet object
        return parseResponseBody(response, HTTP_OK);
    }

    /**
     * Delete an entity(Tweet) by its ID
     *
     * @param s of the entity to be deleted
     * @return deleted entity
     */
    @Override
    public Tweet deleteById(String s) throws URISyntaxException {
        //Construct URI

        StringBuilder sb = new StringBuilder();
        sb.append(API_BASE_URI)
                .append(DELETE_PATH)
                .append(s)
                .append(".json");

        URI uri = new URI(sb.toString());

        // Execute HTTP Request
        HttpResponse response = httpHelper.httpPost(uri);

        //Validate response and deser response to Tweet object
        return parseResponseBody(response, HTTP_OK);
    }

    /**
     * Check response status code Convert Response Entity to Tweet
     */
    Tweet parseResponseBody(HttpResponse response, Integer expectedStatusCode) {
        Tweet tweet = null;

        //Check response status
        int status = response.getStatusLine().getStatusCode();
        if (status != expectedStatusCode){
            try{
                TwitterDao.logger.error(EntityUtils.toString(response.getEntity()));
            } catch (IOException e) {
                TwitterDao.logger.error("Response has no entity", e);
            }
            throw new RuntimeException("Unexpected HTTP status" + status);
        }

        if (response.getEntity() == null) {
            throw new RuntimeException("Empty response body");
        }

        // Convert Response Entity to str
        String jsonStr;
        try {
            jsonStr = EntityUtils.toString(response.getEntity());
        } catch (IOException e){
            throw new RuntimeException("Failed to convert entity to String", e);
        }

        // Deser JSON string to Tweet object
        try {
            tweet = JsonUtil.toObjectFromJson(jsonStr, Tweet.class);
        }catch (IOException e) {
            throw new RuntimeException("Unable to convert jSON str to Object", e);
        }

        return tweet;
    }
}
