package com.mycompany.app;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.MediaType;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

// for decoding Base64 images
import java.util.Base64;
import java.io.ByteArrayInputStream;


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

            // Send the extracted HTML to ChatGPT API
            String jsonResponse = sendToChatGPT(bodyHtml);
            
            // Print the response from the API
            System.out.println("ChatGPT API Response:\n" + jsonResponse);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }





    public static String sendToChatGPT(String htmlContent) {
        OkHttpClient client = new OkHttpClient();
    
        // Construct the prompt
        String prompt = "Extract the following information from the HTML content: " +
                        "Event Name, Logo, Website, Description, Start Date, End Date, Event Type, Format, " +
                        "Social Media Links (Facebook, LinkedIn, Instagram, Twitter), Venue Name, Audience, " +
                        "Venue Details (Name, Website, Phone, Email, Address, Postal Code, City, Country, Region), " +
                        "and contact information if available.";
    
        // Construct the JSON request body for ChatGPT
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "gpt-4");
        JSONArray messagesArray = new JSONArray();
        messagesArray.put(new JSONObject().put("role", "system").put("content", "You are a helpful assistant."));
        messagesArray.put(new JSONObject().put("role", "user").put("content", prompt + "\n\nHTML Content:\n" + htmlContent));
        requestBody.put("messages", messagesArray);
    
        // Create the request to send to OpenAI API
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
    
                // If the response is successful, return the body
                if (response.isSuccessful()) {
                    return response.body().string();
                } else if (response.code() == 429) {
                    System.out.println("Rate limit hit, waiting...");
                    Thread.sleep(waitTime);
                    waitTime *= 2;  // Exponential backoff
                    retries++;
                } else {
                    System.out.println("API call failed with code: " + response.code());
                    return null;
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                return null;
            }
        }
    
        System.out.println("Max retries reached.");
        return null;
    }
    
    
            
    
}