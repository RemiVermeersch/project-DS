package be.kuleuven.distributedsystems.cloud.controller;

import be.kuleuven.distributedsystems.cloud.Model;
import be.kuleuven.distributedsystems.cloud.POJO.Root;
import be.kuleuven.distributedsystems.cloud.PubSubApplication;
import be.kuleuven.distributedsystems.cloud.entities.Quote;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/api")
public class APIController {
    private final Model model;
    private final String PROJECT_ID="demo-distributed-systems-kul";
    private final String BOOKING_TOPIC="booking";
    private final String BOOKING_SUBSCRIPTION = "booking_sub";

    @Autowired
    public APIController(Model model) throws IOException {
        PubSubApplication.createTopicExample(PROJECT_ID,BOOKING_TOPIC);
        PubSubApplication.createPushSubscription(PROJECT_ID,BOOKING_SUBSCRIPTION,BOOKING_TOPIC,"http://localhost:8080/pubsub/bookingsubscription");
        this.model = model;
    }

    @PostMapping(path = "/addToCart", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public ResponseEntity<Void> addToCart(
            @ModelAttribute Quote quote,
            @RequestHeader(value = "referer") String referer,
            @CookieValue(value = "cart", required = false) String cartString) {
        List<Quote> cart = Cart.fromCookie(cartString);
        cart.add(quote);
        ResponseCookie cookie = Cart.toCookie(cart);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
        headers.add(HttpHeaders.LOCATION, referer);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @PostMapping("/removeFromCart")
    public ResponseEntity<Void> removeFromCart(
            @ModelAttribute Quote quote,
            @RequestHeader(value = "referer") String referer,
            @CookieValue(value = "cart", required = false) String cartString) {
        List<Quote> cart = Cart.fromCookie(cartString);
        cart.remove(quote);
        ResponseCookie cookie = Cart.toCookie(cart);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
        headers.add(HttpHeaders.LOCATION, referer);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @PostMapping("/confirmCart")
    public ResponseEntity<Void> confirmCart(
            @CookieValue(value = "cart", required = false) String cartString) throws Exception {
        /***
         * We only implement pub/sub here because there is no need for an immediate
         * answer
         ***/
        PubSubApplication.publishMessage(PROJECT_ID,BOOKING_TOPIC,cartString);
        List<Quote> cart = Cart.fromCookie(cartString);
        cart.clear();
        ResponseCookie cookie = Cart.toCookie(cart);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
        headers.add(HttpHeaders.LOCATION, "/account");
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
