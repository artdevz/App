package com.Beevago.App.controllers;

import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.Beevago.App.dto.HotelDTO;
import com.Beevago.App.dto.ReservationDTO;
import com.Beevago.App.dto.RoomDTO;
import com.Beevago.App.dto.UserDTO;
import com.Beevago.App.enums.ERoomType;
import com.Beevago.App.models.ReservationModel;
import com.Beevago.App.models.RoomModel;
import com.Beevago.App.services.CookieService;
import com.Beevago.App.services.HotelService;
import com.Beevago.App.services.ReservationService;
import com.Beevago.App.services.RoomService;
import com.Beevago.App.services.UserService;
import com.Beevago.App.utils.JwtDecodeToken;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class ReservationController {

    @Autowired
    HotelService hs;

    @Autowired
    ReservationService rs;

    @Autowired
    RoomService qs;

    @Autowired
    UserService us;

    @GetMapping("reservations")
    public ModelAndView listReservations(@RequestParam("userid") UUID userId, RedirectAttributes attributes, HttpServletRequest request) {
        ModelAndView mv = new ModelAndView();
        mv.setViewName("reserva/index");

        if ( (userId == null) || (CookieService.getCookie(request, "JWT") == null) ) {
            mv.setViewName("redirect:/login"); return mv;
        }

        if ( !( us.findEmailById(userId).equals(JwtDecodeToken.getEmailByJwtToken(CookieService.getCookie(request, "JWT"))) )) {
            attributes.addFlashAttribute("errorMessage", "ACESSO NEGADO");
            mv.setViewName("redirect:/login");
            return mv;
        }

        mv.addObject("ReservationsList", rs.findAllReservationsByUserId(userId));
        return mv;
    }

    @PostMapping("/buscar")
    public ModelAndView searchHotels(
        @RequestParam(value = "searchcity", required = false) String hotelCity,
        @RequestParam(value = "categoryfilter", required = false) ERoomType roomType,
        @RequestParam(value = "searchperson", required = false) int personCapacity,
        @RequestParam(value = "searchprice", required = false) double maximumPrice,
        @RequestParam(value = "searchcheckin", required = false) String searchCheckInDate,
        @RequestParam(value = "searchcheckout", required = false) String searchCheckOutDate
        ) throws ParseException {

        ModelAndView mv = new ModelAndView();        
        mv.setViewName("home/index");        
        mv.addObject("categoriesList", ERoomType.values());
        mv.addObject("currentDate", new Date(System.currentTimeMillis()));
        
        if (searchCheckInDate == "" || searchCheckOutDate == "") {            
            mv.setViewName("redirect:/");            
            return mv;
        }

        mv.addObject("stringSearch", rs.generateSearchString(hotelCity, roomType, personCapacity, maximumPrice, searchCheckInDate, searchCheckOutDate));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        java.util.Date checkIn = sdf.parse(searchCheckInDate); java.util.Date checkOut = sdf.parse(searchCheckOutDate);
        List<RoomModel> rooms = qs.findAllRooms(hotelCity, roomType, personCapacity, maximumPrice, new java.sql.Date(checkIn.getTime()), new java.sql.Date(checkOut.getTime()));
        mv.addObject("RoomsList", rooms);        

        return mv;
    }

    @GetMapping("/reservar")
    public ModelAndView getReservaHotelPage(@RequestParam(value = "userid", required = false) UUID userId, @RequestParam("hotelid") UUID hotelId, @RequestParam("roomid") UUID roomId) {
        ModelAndView mv = new ModelAndView();
        if (userId == null) {
            mv.setViewName("redirect:/login"); return mv;
        }
        mv.addObject("user", new UserDTO(userId));        
        mv.addObject("hotel", new HotelDTO(hs.findHotelNameById(hotelId), hotelId));
        mv.addObject("room", new RoomDTO(roomId));
        mv.addObject("reserva", new ReservationDTO(new Date(2l), new Date(2l), 0));
        mv.setViewName("reserva/reservar");
        return mv;
    }

    @PostMapping("/reservarquarto")
    public ModelAndView reservandoHotel(ReservationDTO reserva, @RequestParam("userid") UUID userId, @RequestParam("hotelid") UUID hotelId, @RequestParam("roomid") UUID roomId,BindingResult result, RedirectAttributes attributes) {
        ModelAndView mv = new ModelAndView();

        try {

            if (rs.dateConflicts(roomId, reserva.checkIn(), reserva.checkOut())) {
                attributes.addFlashAttribute("errorMessage", "Quarto indisponível durante esse período.");
                mv.setViewName("redirect:/reservar?userid=" + userId + "&hotelid=" + hotelId + "&roomid=" + roomId);
                return mv;
            }

            if (qs.findCapacityById(roomId) < reserva.qntDePessoas()) {
                attributes.addFlashAttribute("errorMessage", "Quantidade de Pessoas é maior que a Capacidade do Quarto: [" + qs.findCapacityById(roomId) + "].");
                mv.setViewName("redirect:/reservar?userid=" + userId + "&hotelid=" + hotelId + "&roomid=" + roomId);
                return mv;
            }

        } catch (Exception e) {
            attributes.addFlashAttribute("errorMessage", e.getMessage());
            mv.setViewName("redirect:/reservar?userid=" + userId + "&hotelid=" + hotelId + "&roomid=" + roomId);
            return mv;
        }
        
        ReservationModel reservation = new ReservationModel(reserva.checkIn(), reserva.checkOut(), reserva.qntDePessoas());
        reservation.setUserId(userId);        
        reservation.setHotelId(hotelId); 
        reservation.setRoomId(roomId);       
        reservation.setTotalPrice( rs.daysInRoom(reservation.getCheckInDate(), reservation.getCheckOutDate()) * reservation.getQuantidadeDePessoas() * qs.findPriceById(roomId) );
        
        try {
            rs.saveReservation(reservation);
        } catch (Exception e) {
            attributes.addFlashAttribute("errorMessage", e.getMessage());
            mv.setViewName("redirect:/reservar?userid=" + userId + "&hotelid=" + hotelId + "&roomid=" + roomId);
            return mv;
        }
        
        mv.setViewName("redirect:/reservations?userid=" + userId);

        return mv;
    }

}
