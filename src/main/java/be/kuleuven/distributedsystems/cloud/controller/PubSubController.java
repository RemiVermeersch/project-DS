package be.kuleuven.distributedsystems.cloud.controller;

import be.kuleuven.distributedsystems.cloud.Model;
import be.kuleuven.distributedsystems.cloud.POJO.Root;
import be.kuleuven.distributedsystems.cloud.PubSubApplication;
import be.kuleuven.distributedsystems.cloud.entities.Quote;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/pubsub")
public class PubSubController {
    private final Model model;

    @Autowired
    public PubSubController(Model model) throws IOException {
        this.model = model;
    }

    @PostMapping("/bookingsubscription")
    public ResponseEntity<Void> BookingSubscription(@RequestBody String body) throws JsonProcessingException {
        ObjectMapper om = new ObjectMapper();
        be.kuleuven.distributedsystems.cloud.POJO.Root root = om.readValue(body, Root.class);
        List<Quote> cart = Cart.fromCookie(new String(Base64.getDecoder().decode(root.message.data)));
        this.model.confirmQuotes(new ArrayList<>(cart), root.message.attributes.user);
        return ResponseEntity.ok().build();
    }
}
