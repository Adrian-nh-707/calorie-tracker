package com.group19.calorietrack.controller;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.boot.test.context.SpringBootTest;

import com.group19.calorietrack.repositories.FoodRepository;
import com.group19.calorietrack.models.Food;
import static org.mockito.Mockito.when;
import org.hamcrest.Matchers;

import static org.hamcrest.Matchers.*;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
public class FoodControllerTest {

    public static final MediaType APPLICATION_JSON_UTF8 = new MediaType(MediaType.APPLICATION_JSON.getType(), MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));
    
    @MockitoBean
    private FoodRepository foodRepository;
    
    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetAllFood() throws Exception {

        // User u1 = new User();
        // u1.setName("bobby");
        // u1.setPassword("1234");
        // u1.setSize(42);

        // User u2 = new User();
        // u2.setName("jane");
        // u2.setPassword("1234");
        // u2.setSize(25);

        // List<User> users = new ArrayList<User>();
        // users.add(u1);
        // users.add(u2);

        // when(userRepository.findAll()).thenReturn(users);

        // mockMvc.perform(MockMvcRequestBuilders.get("/users/view"))
        //     .andExpect(MockMvcResultMatchers.status().isOk())
        //     .andExpect(MockMvcResultMatchers.view().name("users/showAll"))
            
        //     .andExpect(MockMvcResultMatchers.model().attribute("us", hasItem(
        //         allOf(
        //             hasProperty("name", Matchers.is("bobby")),
        //             hasProperty("password", Matchers.is("1234")),
        //             hasProperty("size", Matchers.is(42))
        //         )
        //     )));
    }

}