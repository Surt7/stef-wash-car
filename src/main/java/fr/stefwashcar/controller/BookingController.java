package fr.stefwashcar.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BookingController {

    @GetMapping("/booking")
    public String index(Model model) {
        model.addAttribute("controller_name", "BookingController");
        return "booking/index";
    }
}
