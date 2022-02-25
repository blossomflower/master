package com.attendence.management.controller;

import java.math.BigDecimal;
import java.security.Principal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.attendence.management.domain.Attendence;
import com.attendence.management.domain.Student;
import com.attendence.management.domain.Teacher;
import com.attendence.management.domain.User;
import com.attendence.management.dto.StudentAttendenceDTO;
import com.attendence.management.repository.AttendenceRepository;
import com.attendence.management.repository.StudentRepository;
import com.attendence.management.repository.TeacherRepository;
import com.attendence.management.repository.UserRepository;
import com.attendence.management.service.CustomPasswordEnconder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.neo4j.Neo4jProperties.Authentication;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

@Controller

public class WebController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private AttendenceRepository attendenceRepository;

    @Autowired
    private StudentRepository studentRepository;

    @GetMapping("")
    public String viewHomePage() {
        return "index";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());

        return "signup_form";
    }

    @PostMapping("/process_register")
    public String processRegister(User user, BindingResult bindingResult) {
        if (bindingResult.hasErrors() || (userRepository.findByUsername(user.getUsername()) != null)) {

            FieldError error = new FieldError("user", "username",
                    "Username should be unique");
            bindingResult.addError(error);
            return "signup_form";
        }
        if(user.getType().equalsIgnoreCase("STUDENT")){
            Student student = studentRepository.findByStudentId(user.getUserId());
            if(student == null){
                FieldError error = new FieldError("user", "userId",
                "Please select valid Student Id as User Id");
        bindingResult.addError(error);
        return "signup_form";
            }
        }else{
            Teacher teacher = teacherRepository.findByTeacherId(user.getUserId());
            if(teacher == null){
                FieldError error = new FieldError("user", "userId",
                "Please select valid Teacher Id as User Id");
        bindingResult.addError(error);
        return "signup_form";
            }
        }
        if (bindingResult.hasErrors()
                || (userRepository.findByUserIdAndType(user.getUserId(), user.getType()) != null)) {

            FieldError error = new FieldError("user", "userId",
                    "User Id is already registered");
            bindingResult.addError(error);
            return "signup_form";
        }

        CustomPasswordEnconder passwordEncoder = new CustomPasswordEnconder();
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        userRepository.save(user);

        return "resgister_success";
    }

    @RequestMapping("/default")
    public String defaultAfterLogin(HttpServletRequest request) {
        Principal principle = request.getUserPrincipal();
        User user = userRepository.findByUsername(principle.getName());
        if (user.getUsername().equalsIgnoreCase("ADMIN")) {
            return "redirect:/admin/";
        }
        if (user.getType().equalsIgnoreCase("TEACHER")) {
            return "redirect:/faculty/";
        }
        return "redirect:/student/";
    }

    @RequestMapping("/faculty")
    public ModelAndView facultyLogin(HttpServletRequest request) {
        Principal principle = request.getUserPrincipal();
        User user = userRepository.findByUsername(principle.getName());

        Teacher teacher = teacherRepository.findByTeacherId(user.getUserId());
        ModelAndView mav = new ModelAndView("faculty");
        mav.addObject("teacher", teacher);
        return mav;
    }

    @RequestMapping("/student")
    public ModelAndView studentLogin(HttpServletRequest request) {
        Principal principle = request.getUserPrincipal();
        User user = userRepository.findByUsername(principle.getName());

        Student student = studentRepository.findByStudentId(user.getUserId());
        ModelAndView mav = new ModelAndView("student");
        mav.addObject("student", student);
        return mav;
    }


    @RequestMapping("/admin")
    public String adminLogin(HttpServletRequest request) {

        return "admin";
    }

    @PostMapping("/getDataByDateFaculty")
    public @ResponseBody ModelMap getDataByDateFaculty(@RequestParam Date date, @RequestParam String facultyId) {
        ModelMap map = new ModelMap();
        Teacher teacher = teacherRepository.findByTeacherId(facultyId);
        List<StudentAttendenceDTO> dtoList = new ArrayList<StudentAttendenceDTO>();
        for (Student student : teacher.getStudentList()) {
            Attendence attendence = attendenceRepository.findByStudentIdandDate(date, student);

            StudentAttendenceDTO studentAttendenceDTO = new StudentAttendenceDTO();
            studentAttendenceDTO.setName(student.getName());
            studentAttendenceDTO.setClassNo(student.getClassName());
            studentAttendenceDTO.setRollNo(student.getRollNo());
            studentAttendenceDTO.setStudentId(student.getId());
            studentAttendenceDTO.setDate(date);
            studentAttendenceDTO.setTeacherId(facultyId);
            studentAttendenceDTO.setPresent(attendence != null ? attendence.getIsPresent() : false);
            dtoList.add(studentAttendenceDTO);
        }
        long totalStudent = dtoList.size();
        long prestStudentNo = dtoList.stream().filter(studentDTO -> { return studentDTO.getPresent() ;}).count();

        BigDecimal n = new BigDecimal(prestStudentNo);
        BigDecimal d = new BigDecimal(totalStudent);
        BigDecimal i = new BigDecimal(100);
        int percentage = n.multiply(i).divide(d).intValue();
        map.addAttribute("studentAttendenceDTOList", dtoList);
        map.addAttribute("attendencePercentage", percentage);

        return map;

    }

    @PostMapping(value = "/facultyUpdateAttendence")
    public @ResponseBody ModelMap facultySaveAttendenceByDate(@RequestBody String data)
            throws JsonMappingException, JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        List<StudentAttendenceDTO> studentAttendenceDTOList = mapper.readValue(data,
                new TypeReference<List<StudentAttendenceDTO>>() {
                });

        ModelMap map = new ModelMap();
        for (StudentAttendenceDTO attendenceDTO : studentAttendenceDTOList) {
            Student student = studentRepository.getById(attendenceDTO.getStudentId());
            Attendence attendence = attendenceRepository.findByStudentIdandDate(attendenceDTO.getDate(), student);
            if (attendence == null)
                attendence = new Attendence();
            attendence.setDate(attendenceDTO.getDate());
            attendence.setIsPresent(attendenceDTO.getPresent());
            attendence.setStudent(studentRepository.getById(attendenceDTO.getStudentId()));
            attendenceRepository.save(attendence);
        }

        long totalStudent = studentAttendenceDTOList.size();
        long prestStudentNo = studentAttendenceDTOList.stream().filter(studentDTO -> { return studentDTO.getPresent() ;}).count();

        BigDecimal n = new BigDecimal(prestStudentNo);
        BigDecimal d = new BigDecimal(totalStudent);
        BigDecimal i = new BigDecimal(100);
        int percentage = n.multiply(i).divide(d).intValue();

        map.addAttribute("studentAttendenceDTOList", studentAttendenceDTOList);
        map.addAttribute("attendencePercentage", percentage);
        return map;
    }

    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    public String logoutPage(HttpServletRequest request, HttpServletResponse response) {
        org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        return "redirect:/";
    }

    @PostMapping("/addTeacher")
    public String addTeacherr(@Validated Teacher teacher, BindingResult result, Model model) {
        if (result.hasErrors() || (teacherRepository.findByTeacherId(teacher.getTeacherId()) != null)) {
            FieldError error = new FieldError("teacher", "teacherId",
                    "Teacher Id is already added");
            result.addError(error);
            return "add-teacher";
        }

        teacherRepository.save(teacher);
        return "redirect:/getAllTeachers/";
    }

    @GetMapping("/addTeacher")
    public String addTeacher(Model model) {
        model.addAttribute("teacher", new Teacher());

        return "add-teacher";
    }

    @GetMapping("/getAllTeachers")
    public String showTeacherList(Model model) {
        model.addAttribute("teachers", teacherRepository.findAll());
        return "show-teachers";
    }

    @RequestMapping("/editTeacher/{id}")
    public String showUpdateTeacherForm(@PathVariable("id") long id, Model model) {
        Teacher user = teacherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Teacher Id:" + id));

        model.addAttribute("teacher", user);
        return "update-teacher";
    }

    @PostMapping("/updateTeacher/{id}")
    public String updateTeacher(@PathVariable("id") long id, @Validated Teacher teacher,
            BindingResult result, Model model) {
        Teacher otherTeacher = teacherRepository.findByTeacherId(teacher.getTeacherId());
        if (result.hasErrors() || (otherTeacher != null && otherTeacher.getId() != teacher.getId())) {
            FieldError error = new FieldError("teacher", "teacherId",
                    "Teacher Id is already added");
            result.addError(error);

            teacher.setId(id);
            return "update-teacher";
        }

        teacherRepository.save(teacher);
        return "redirect:/getAllTeachers/";
    }

    @GetMapping("/deleteTeacher/{id}")
    public String deleteTeacher(@PathVariable("id") long id, Model model) {
        Teacher user = teacherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Teacher Id:" + id));
        teacherRepository.delete(user);
        return "redirect:/getAllTeachers/";
    }

    @PostMapping("/addStudent")
    public String addStudent(@Validated Student student, BindingResult result, Model model) {
        Student studentOther = studentRepository.findByStudentId(student.getStudentId());
        Teacher teacher = teacherRepository.findByTeacherId(student.getTeacherId());

        if (result.hasErrors() || (studentOther != null && studentOther.getId() != studentOther.getId())) {
            FieldError error = new FieldError("student", "studentId",
                    "Student Id is already added");
            result.addError(error);

            return "add-student";
        }

        if (result.hasErrors() || (teacher == null)) {
            FieldError error = new FieldError("student", "teacherId",
                    "Please enter valid Teacher Id");
            result.addError(error);

            return "add-student";
        }
        student.setTeacher(teacher);
        studentRepository.save(student);
        return "redirect:/getAllStudents/";
    }

    @GetMapping("/addStudent")
    public String addStudent(Model model) {
        model.addAttribute("student", new Student());

        return "add-student";
    }

    @GetMapping("/getAllStudents")
    public String showStudentList(Model model) {
       List<Student> students =  studentRepository.findAll();
       students.stream().forEach(student -> {student.setTeacherId(student.getTeacher().getTeacherId());});
        model.addAttribute("students",students);
        return "show-students";
    }

    @RequestMapping("/editStudent/{id}")
    public String showUpdateStudentrForm(@PathVariable("id") long id, Model model) {
        Student user = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Student Id:" + id));
                user.setTeacherId(user.getTeacher().getTeacherId());
        model.addAttribute("student", user);
        return "update-student";
    }

    @PostMapping("/updateStudent/{id}")
    public String updateStudent(@PathVariable("id") long id, @Validated Student student,
            BindingResult result, Model model) {
        Student studentOther = studentRepository.findByStudentId(student.getStudentId());
        Teacher teacher = teacherRepository.findByTeacherId(student.getTeacherId());

        if (result.hasErrors() || (studentOther != null && studentOther.getId() != studentOther.getId())) {
            FieldError error = new FieldError("student", "studentId",
                    "Student Id is already added");
            result.addError(error);

            student.setId(id);
            return "update-student";
        }

        if (result.hasErrors() || (teacher == null)) {
            FieldError error = new FieldError("student", "teacherId",
                    "Please enter valid Teacher Id");
            result.addError(error);

            student.setId(id);
            return "update-student";
        }

        student.setTeacher(teacher);
        studentRepository.save(student);
        return "redirect:/getAllStudents/";
    }

    @GetMapping("/deleteStudent/{id}")
    public String deleteStudent(@PathVariable("id") long id, Model model) {
        Student user = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Student Id:" + id));
        studentRepository.delete(user);
        return "redirect:/getAllStudents/";
    }

    @PostMapping("/getDataByDateStudent")
    public @ResponseBody ModelMap getDataByDateStudent(@RequestParam Date date, @RequestParam String studentId) {
        ModelMap map = new ModelMap();
        Student student = studentRepository.findByStudentId(studentId);
        List<StudentAttendenceDTO> dtoList = new ArrayList<StudentAttendenceDTO>();
       
            Attendence attendence = attendenceRepository.findByStudentIdandDate(date, student);

            StudentAttendenceDTO studentAttendenceDTO = new StudentAttendenceDTO();
            studentAttendenceDTO.setName(student.getName());
            studentAttendenceDTO.setClassNo(student.getClassName());
            studentAttendenceDTO.setRollNo(student.getRollNo());
            studentAttendenceDTO.setStudentId(student.getId());
            studentAttendenceDTO.setDate(date);
            studentAttendenceDTO.setTeacherId(student.getTeacher().getTeacherId());
            studentAttendenceDTO.setPresent(attendence != null ? attendence.getIsPresent() : false);
            dtoList.add(studentAttendenceDTO);
        
      
        map.addAttribute("studentAttendenceDTOList", dtoList);

        return map;

    }


}
