package mine.imageweb.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import mine.imageweb.entity.Album;
import mine.imageweb.entity.Comment;
import mine.imageweb.entity.Follow;
import mine.imageweb.entity.Image;
import mine.imageweb.entity.Notify;
import mine.imageweb.entity.Post;
import mine.imageweb.entity.Role;
import mine.imageweb.entity.RoleName;
import mine.imageweb.entity.User;
import mine.imageweb.exception.AppException;
import mine.imageweb.repository.AlbumRepository;
import mine.imageweb.repository.CommentRepository;
import mine.imageweb.repository.FollowRepository;
import mine.imageweb.repository.ImageRepository;
import mine.imageweb.repository.NotifyRepository;
import mine.imageweb.repository.PostRepository;
import mine.imageweb.repository.RoleRepository;
import mine.imageweb.repository.UserRepository;
import mine.imageweb.request.ChangeInfoRequest;
import mine.imageweb.request.CreatePostRequest;
import mine.imageweb.request.LoginRequest;
import mine.imageweb.request.PostImage;
import mine.imageweb.request.SignUpRequest;
import mine.imageweb.secure.JwtTokenProvider;
import mine.imageweb.service.GoogleDriveService;



	private User getUser(Model model) {
		HttpSession session = httpSessionFactory.getObject();
		try {
			Long id = tokenProvider.getUserIdFromJWT(session.getAttribute("accessToken").toString());
			User user = userRepository.findById(id).orElse(null);
			return user;
		} catch (Exception e) {
			return null;
		}
	}

	private List<Notify> getNotify(User user, int limit) {
		if (user == null)
			return null;
		List<Notify> notifyList = notifyRepository.getList(user.getId(), limit);
		return notifyList;
	}

	private Model setModel(Model model) {
		List<Album> albumList = albumRepository.findAll();

		model.addAttribute("albumList", albumList);
		model.addAttribute("createPostRequest", new CreatePostRequest());
		model.addAttribute("loginRequest", new LoginRequest());
		model.addAttribute("signupRequest", new SignUpRequest());
		User user = getUser(model);
		if (user != null) {
			model.addAttribute("currentUserID", user.getId());
			Role role = roleRepository.findByUserID(user.getId()).get(0);
			System.out.println(role.getName());
			model.addAttribute("role", role.getId());
		}
		else
			model.addAttribute("currentUserID", false);

		List<Notify> notifyList = getNotify(user, 10);
		
		model.addAttribute("notifyList", notifyList);

		return model;

	}

	private void setFollow(User user, Post post) {
		Follow follow = new Follow(null, post, user);
		followRepository.save(follow);
	}

	private boolean isRole(Model model, RoleName roleName) {
		User user = getUser(model);

		List<Role> roles = roleRepository.findByUserID(user.getId());

		for (Role role : roles) {
			if (role.getName().equals(roleName))
				return true;
		}
		return false;
	}



	



	@PostMapping("/newComment")
	public String createComment(@RequestParam("newComment-postID") String postID,
			@RequestParam("newComment-content") String content, Model model) {

		model = setModel(model);

		User user = getUser(model);
		Post post = postRepository.findById(Long.valueOf(postID)).orElse(null);

		Comment comment = new Comment(null, content, post, user);
		commentRepository.save(comment);

		if (followRepository.checkFollow(post.getId(), user.getId()).size() == 0)
			setFollow(user, post);

		List<Follow> followList = followRepository.findByPostID(post.getId());
		for (Follow follow : followList) {
			if (follow.getUser() != user) {
				notifyRepository.save(new Notify(null, "New comment", post, user));
			}
		}

		return redirectPost(post.getId(), model);
	}

	@PostMapping("report")
	public String report(@RequestParam("postID") String postID, Model model) {
		model = setModel(model);

		Post post = postRepository.findById(Long.valueOf(postID)).orElse(null);
		if (post == null)
			return homePage(model, null);

		List<User> adminList = userRepository.findByRoleID(Long.valueOf(2));
		for (User user : adminList) {
			notifyRepository.save(new Notify(null, "Report", post, user));
		}

		return homePage(model, null);
	}

	@PostMapping("lock")
	public String lock(@RequestParam("postID") String postID, Model model) {
		model = setModel(model);
		Post post = postRepository.findById(Long.valueOf(postID)).orElse(null);
		if (post != null)
			postRepository.delete(post);
		return homePage(model, null);
	}
}