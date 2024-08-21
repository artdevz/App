package com.Bivago.App.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.Bivago.App.dto.HotelDTO;
import com.Bivago.App.models.RoomModel;
import com.Bivago.App.services.HotelService;
import com.Bivago.App.services.RoomService;

import jakarta.servlet.http.HttpSession;

@Controller
public class RoomController {

    @Autowired
    RoomService rs;

    @Autowired
    HotelService hs;
    
    @GetMapping("adminhotel/roomsettings/{id}")
    public ModelAndView getRoomSettingsPage(@PathVariable UUID id) {
        ModelAndView mv = new ModelAndView();
        mv.setViewName("room/index");             
        mv.addObject("HotelName", new HotelDTO(hs.findHotelNameById(id)));
        // List<RoomModel> rooms = rs.findRoomsOfHotel(hotel.getId());
        // List<RoomModel> rooms = rs.findRoomsOfHotel();
        List<RoomModel> rooms = rs.findAllRooms();
        mv.addObject("RoomsList", rooms);        
        return mv;
    }

    @GetMapping("adminhotel/roomsettings/cadastroquatro")
    public ModelAndView getRegisterRoomPage() {
        ModelAndView mv = new ModelAndView();
        mv.addObject("room", new RoomModel());
        mv.setViewName("room/register");
        return mv;
    }

    @PostMapping("/cadastrarquarto")
    public ModelAndView cadastrarQuarto(RoomModel room, BindingResult result, HttpSession session, RedirectAttributes attributes) throws Exception {
        ModelAndView mv = new ModelAndView();

        if (result.hasErrors()) {
            attributes.addFlashAttribute("msg_erro", "ERRO! Verifique se há campos em branco.");
            mv.setViewName("redirect:/adminhotel/roomsettings");            
            return mv;
        }

        rs.saveRoom(room);
        session.setAttribute("quartoCadastrado", room);
        attributes.addFlashAttribute("msg", "Quarto cadastrado com Sucesso!");        
        mv.setViewName("redirect:/adminhotel/roomsettings");

        return mv;
    }

}
