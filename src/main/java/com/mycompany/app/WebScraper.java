package com.mycompany.app;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
// import org.jsoup.select.Elements;
// import org.jsoup.nodes.Element;
// import net.sourceforge.tess4j.ITesseract;
// import net.sourceforge.tess4j.Tesseract;
// import net.sourceforge.tess4j.TesseractException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.MediaType;

// import java.io.File;
import java.io.IOException;
// import java.net.URL;
// import javax.imageio.ImageIO;
// import java.awt.image.BufferedImage;

// for decoding Base64 images
// import java.util.Base64;
// import java.io.ByteArrayInputStream;


import io.github.cdimascio.dotenv.Dotenv;


public class WebScraper {

    private static final Dotenv dotenv = Dotenv.load();
    private static final String API_KEY = dotenv.get("OPENAI_API_KEY");
    
    // URL for OpenAI's GPT-4
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";

    
    public static void main(String[] args) {
        // URL of the website to scrape
        String url = "https://www.worldhealthsummit.org/"; // text based
        // String url = "https://healthandwellnessexpo.com/";    // image based

        try {
            // Connect to the website and get the HTML document
            Document doc = Jsoup.connect(url).get();

            // Remove script, style, and noscript elements so we don't use a lot of API credits
            doc.select("script, style, noscript").remove();
            
            // Get the body HTML without those elements
            String bodyHtml = doc.body().html();

            // Print the body HTML for debugging purposes
            System.out.println("HTML extracted: \n" + bodyHtml);
            System.out.println("************************************************************************\n\n\n\n\n");

            // Initialize ChatGPT with the first prompt (example prompt)
            System.out.println("Initializing ChatGPT with example prompt...\n");
            String firstPrompt = initializeFirstPrompt();  // Call to first prompt
            String firstResponse = sendPromptToChatGPT(firstPrompt);
            System.out.println("ChatGPT Response for Initialization: \n" + firstResponse);

            // Use the second prompt to scrape the actual HTML data
            System.out.println("\nScraping actual HTML data from website...");
            String secondPrompt = createSecondPrompt(bodyHtml);  // Call to second prompt
            String secondResponse = sendPromptToChatGPT(secondPrompt);

            // Print the response from ChatGPT (scraped data)
            if (secondResponse != null) {
                System.out.println("ChatGPT Response for Scraping: \n" + secondResponse);
            } else {
                System.out.println("No valid response from ChatGPT.");
            }



        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Function to send a prompt to ChatGPT and get the response
    public static String sendPromptToChatGPT(String prompt) {
        // Create OkHttpClient instance with timeouts
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        // Build the request body for OpenAI API
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "gpt-4");
        JSONArray messagesArray = new JSONArray();
        messagesArray.put(new JSONObject().put("role", "system").put("content", 
            "You are an expert web scraper specializing in extracting event-related information from HTML content. " +
            "Your job is to analyze the provided HTML and return structured event data such as event name, date, description, " +
            "social media links, and venue information in JSON format. If any of the required data is not available in the HTML, " +
            "set that field to `null`."));
        messagesArray.put(new JSONObject().put("role", "user").put("content", prompt));
        requestBody.put("messages", messagesArray);

        RequestBody body = RequestBody.create(requestBody.toString(), MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .build();

        int retries = 0;
        int maxRetries = 5;
        int waitTime = 1000;  // Initial wait time in milliseconds

        while (retries < maxRetries) {
            try {
                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    return response.body().string();
                } else if (response.code() == 429) {
                    System.out.println("Rate limit hit, waiting...");
                    Thread.sleep(waitTime);
                    waitTime *= 2;  // Exponential backoff
                    retries++;
                } else {
                    System.out.println("Error: " + response.code() + " " + response.message());
                    return null;
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                return null;
            }
        }

        System.out.println("Max retries reached. Unable to get a valid response.");
        return null;
    }


    // Function to create the first initialization prompt with an example
    public static String initializeFirstPrompt() {
        String exampleHtml = "<html><head><title>World Health Summit 2024</title></head><body>" +
                "<h1>World Health Summit 2024</h1>" +
                "<img src='https://www.worldhealthsummit.org/fileadmin/headerimages/WHS_logo.svg' alt='WHS Logo'>" +
                "<p>Join the global health community at the World Health Summit 2024, from October 13-15 in Berlin, Germany.</p>" +
                "<p>Date: October 13-15, 2024</p>" +
                "<a href='https://www.worldhealthsummit.org/'>Visit Website</a>" +
                "<div class='social'>" +
                "<a href='https://www.facebook.com/worldhealthsummit'>Facebook</a>" +
                "<a href='https://www.instagram.com/worldhealthsummit'>Instagram</a>" +
                "<a href='https://twitter.com/worldhealthsmt'>Twitter</a>" +
                "</div></body></html>";

        String prompt = "Hello! I need you to extract specific event data from the HTML of websites and provide the data in a structured JSON format. " +
                "Here's an example of what we're looking for:\n\n" +
                "Example HTML:\n" + exampleHtml + "\n\n" +
                "Desired Output Format in JSON:\n" +
                "{ \"event\": { \"name\": \"World Health Summit 2024\", \"logo\": \"https://www.worldhealthsummit.org/fileadmin/headerimages/WHS_logo.svg\", " +
                "\"website\": \"https://www.worldhealthsummit.org/\", \"description\": \"Join the global health community at the World Health Summit 2024, " +
                "from October 13-15 in Berlin, Germany.\", \"start_date\": \"October 13, 2024\", \"end_date\": \"October 15, 2024\", " +
                "\"event_type\": \"Conference\", \"format\": \"In-person\", \"facebook\": \"https://www.facebook.com/worldhealthsummit\", " +
                "\"instagram\": \"https://www.instagram.com/worldhealthsummit\", \"twitter\": \"https://twitter.com/worldhealthsmt\", \"venue_name\": \"Berlin, Germany\" }}\n" +
                "\nIn future interactions, I will provide more HTML content, and I would like you to extract the same information and return it in this JSON format.";
        
        return prompt;
    }

    // Function to create the second prompt for future scraping based on the actual HTML
    public static String createSecondPrompt(String htmlContent) {
        String prompt = "Here is the HTML content of a webpage. Please extract the event information and return it in the following JSON format:\n\n" +
                "{ \"event\": { \"name\": \"\", \"logo\": \"\", \"website\": \"\", \"description\": \"\", \"start_date\": \"\", \"end_date\": \"\", " +
                "\"event_type\": \"\", \"format\": \"\", \"facebook\": \"\", \"instagram\": \"\", \"twitter\": \"\", \"venue_name\": \"\" }}\n\n" +
                "HTML Content:\n" + htmlContent;
        return prompt;
    }
}