package mine.imageweb.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.querydsl.QPageRequest;
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
import mine.imageweb.entity.LikeCount;
import mine.imageweb.entity.Notify;
import mine.imageweb.entity.Post;
import mine.imageweb.entity.PostTag;
import mine.imageweb.entity.Role;
import mine.imageweb.entity.RoleName;
import mine.imageweb.entity.Tag;
import mine.imageweb.entity.User;
import mine.imageweb.exception.AppException;
import mine.imageweb.repository.AlbumRepository;
import mine.imageweb.repository.CommentRepository;
import mine.imageweb.repository.FollowRepository;
import mine.imageweb.repository.ImageRepository;
import mine.imageweb.repository.LikeRepository;
import mine.imageweb.repository.NotifyRepository;
import mine.imageweb.repository.PostRepository;
import mine.imageweb.repository.PostTagRepository;
import mine.imageweb.repository.RoleRepository;
import mine.imageweb.repository.TagRepository;
import mine.imageweb.repository.UserRepository;
import mine.imageweb.repository.specification.GenericSpecifications;
import mine.imageweb.request.ChangeInfoRequest;
import mine.imageweb.request.CreatePostRequest;
import mine.imageweb.request.LoginRequest;
import mine.imageweb.request.PostImage;
import mine.imageweb.request.SignUpRequest;
import mine.imageweb.secure.JwtTokenProvider;
import mine.imageweb.service.GoogleDriveService;

import static mine.imageweb.repository.specification.PostTagSpecification.*;

@Controller
public class BaseController {
	@Autowired
	AuthenticationManager authenticationManager;

	@Autowired
	UserRepository userRepository;

	@Autowired
	RoleRepository roleRepository;

	@Autowired
	AlbumRepository albumRepository;

	@Autowired
	PostRepository postRepository;

	@Autowired
	ImageRepository imageRepository;

	@Autowired
	CommentRepository commentRepository;

	@Autowired
	FollowRepository followRepository;

	@Autowired
	NotifyRepository notifyRepository;

	@Autowired
	LikeRepository likeRepository;

	@Autowired
	TagRepository tagRepository;

	@Autowired
	PostTagRepository postTagRepository;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Autowired
	JwtTokenProvider tokenProvider;

	@Autowired
	GoogleDriveService driveService;

	@Autowired
	ObjectFactory<HttpSession> httpSessionFactory;

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

		model.addAttribute("createPostRequest", new CreatePostRequest());
		model.addAttribute("loginRequest", new LoginRequest());
		model.addAttribute("signupRequest", new SignUpRequest());

		// Get current user
		User user = getUser(model);
		List<Notify> notifyList = null;
		// If logined,
		if (user != null) {
			// add userID and user_role into session
			model.addAttribute("currentUserID", user.getId());
			Role role = roleRepository.findByUserID(user.getId()).get(0);
			model.addAttribute("role", role.getId());

			// Get list album from database
			List<Album> albumList = albumRepository.findByUserID(user.getId());
			model.addAttribute("albumList", albumList);

			// Get list notify from database
			notifyList = getNotify(user, 10);

		} else
			// if not login yet
			model.addAttribute("currentUserID", false);

		model.addAttribute("notifyList", notifyList);

		return model;

	}

	private void setFollow(User user, Post post) {
		Follow follow = new Follow(null, post, user);
		followRepository.save(follow);
	}

	@GetMapping(value = { "", "/", "/home" })
	public String homePage(Model model, @RequestParam(value = "search", required = false) String search) {
		model = setModel(model);

		List<Post> postList = new ArrayList<Post>();
		List<PostImage> postImageList = new ArrayList<PostImage>();

		@SuppressWarnings("unchecked")
		Set<String> keySet = (Set<String>) model.getAttribute("keySet");

		if (keySet != null) {
			List<Tag> tagList = new ArrayList<Tag>();
			for (String key : keySet) {
				Tag tag = tagRepository.findByName(key).orElse(null);
				if (tag != null)
					tagList.add(tag);
			}

			Specification<PostTag> specification = Specification.where(hasTagIn(tagList));
			GenericSpecifications<PostTag> genericSpecifications = new GenericSpecifications<PostTag>();
			List<String> joinColumn = new ArrayList<String>();
			joinColumn.add("post");

			specification = genericSpecifications.groupBy(specification, joinColumn);

			Page<PostTag> listPostTag = postTagRepository.findAll(specification, PageRequest.of(0, 8));
			for (PostTag postTag : listPostTag) {
				postList.add(postTag.getPost());
			}

		} else {
			if (search != null) {
				String[] key = search.split(" ");
				search = "%";
				for (String string : key) {
					if (string.length() > 0) {
						search += string + "%";
					}
				}
			} else
				search = new String("%");

			postList = postRepository.findTop(search, 8);

		}
		Random rd = new Random();

		for (Post post : postList) {
			List<Image> imageList = imageRepository.findByPostID(post.getId());

			if (imageList.size() > 0) {
				PostImage postImage = new PostImage(post.getId(),
						imageList.get(rd.nextInt(imageList.size())).getLink());
				postImageList.add(postImage);
			}
		}

		model.addAttribute("postImageList", postImageList);

		return "home";
	}

	@PostMapping("/filter")
	public String filter(@ModelAttribute("filter") String keys, Model model) {
		String keyArr[] = keys.split(" ");
		Set<String> keySet = new HashSet<String>();

		for (String key : keyArr) {
			if (key.length() > 0) {
				keySet.add(key);
			}
		}
		model.addAttribute("keySet", keySet);

		return homePage(model, null);
	}

	// /profile?userID=...
	@GetMapping(value = { "/profile" })
	public String profilePage(@RequestParam(name = "userID", required = false) Long userID, Model model) {
		model = setModel(model);

		User user = null;
		// nếu url không có userID => chủ nhân profile là người dùng hiện tại
		if (userID == null)
			user = getUser(model);
		else
			// lấy thông tin chủ profile theo id
			user = userRepository.findById(userID).orElse(null);

		// nếu không tồn tại, báo lỗi
		if (user == null) {
			model.addAttribute("error", "User not exist!");
			return homePage(model, null);
		}

		model.addAttribute("user", user);

		// lấy danh sách album của người dùng
		List<Album> albumList = albumRepository.findAll();
		model.addAttribute("albumList", albumList);

		List<PostImage> postImageList = new ArrayList<PostImage>();
		List<Post> postList = postRepository.findByUserID(userID);

		Random rd = new Random();
		for (Post post : postList) {
			List<Image> imageList = imageRepository.findByPostID(post.getId());

			if (imageList.size() > 0) {
				PostImage postImage = new PostImage(post.getId(),
						imageList.get(rd.nextInt(imageList.size())).getLink());
				postImageList.add(postImage);
			}
		}

		model.addAttribute("postImageList", postImageList);

		ChangeInfoRequest info = new ChangeInfoRequest(user.getId(), user.getUsername(), user.getInstagram(),
				user.getFacebook(), user.getAddress(), user.getPhone(), user.getEmail(), user.getPassword(),
				user.getAboutMe(), user.getCreatedAt().toString(), user.getAvatar(), user.getBackground());

		model.addAttribute("info", info);

		return "profile";
	}

	@SuppressWarnings("finally")
	@PostMapping(value = "/login")
	public String login(@ModelAttribute("loginRequest") LoginRequest request, Model model) {

		try {
			// kiểm tra người dùng từ database(email + password)
			Authentication authentication = authenticationManager
					.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

			// lưu thông tin người dùng đã đăng nhập
			// xóa thông tin người dùng nếu tắt trình duyệt
			String accessToken = tokenProvider.generateToken(authentication);
			String refreshToken = tokenProvider.generateRefreshToken(authentication);

			HttpSession session = httpSessionFactory.getObject();
			session.setAttribute("accessToken", accessToken);

			Cookie refreshTokenCk = new Cookie("refreshToken", refreshToken);
			// expires in 7 days
			refreshTokenCk.setMaxAge(7 * 24 * 60 * 60);

			/*
			 * optional properties refreshTokenCk.setSecure(true);
			 * refreshTokenCk.setHttpOnly(true);
			 */
			refreshTokenCk.setPath("/");

			// lấy thông tin người dùng hiện tại
			User user = getUser(model);
			session.setAttribute("user", user);
		} catch (Exception e) {
			// e.printStackTrace();
			model.addAttribute("error", "Incorrect email/password!");
		} finally {
			model = setModel(model);
			return homePage(model, null);
		}

	}

	@SuppressWarnings("finally")
	@PostMapping(value = "/signup")
	public String signup(@ModelAttribute("signupRequest") SignUpRequest request, Model model) {

		try {
			// nếu email đã được sử dụng => báo lỗi, chuyển về home
			if (userRepository.existsByEmail(request.getEmail())) {
				model.addAttribute("error", "Email had been used!");
				model = setModel(model);
				return homePage(model, null);
			}
			// Nếu email chưa được sử dụng
			User user = new User(request.getUsername(), request.getEmail(),
					passwordEncoder.encode(request.getPassword()));
			user.setAvatar("https://www.eds.org.nz/assets/img/head%20and%20shoulders%20image%20male.png?k=ed458a5745");

			// cấp quyền cho người dùng. Mặc định là user
			Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
					.orElseThrow(() -> new AppException("User Role not set."));
			user.setRoles(Collections.singleton(userRole));
			userRepository.save(user);
			model.addAttribute("success", "SignUp success!");

			Album album = new Album("My album", user);
			albumRepository.save(album);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			model = setModel(model);
			return homePage(model, null);

		}

	}

	// thay đổi thông tin người dùng
	@PostMapping(value = "/changeInfo")
	public String changeInfo(@ModelAttribute("info") ChangeInfoRequest request, Model model) {
		model = setModel(model);
		User user = getUser(model);

		// nếu nhập đúng mật khẩu
		if (passwordEncoder.matches(request.getPassword(), user.getPassword())) {
			// thay đổi thông tin
			user.setUsername(request.getUsername());
			user.setEmail(request.getEmail());
			user.setAvatar(request.getAvatar());
			user.setBackground(request.getBackground());
			user.setAddress(request.getAddress().length() > 0 ? request.getAddress() : null);
			user.setPhone(request.getPhone().length() > 0 ? request.getPhone() : null);
			user.setInstagram(request.getInstagram().length() > 0 ? request.getInstagram() : null);
			user.setFacebook(request.getFacebook().length() > 0 ? request.getFacebook() : null);
			user.setAboutMe(request.getAboutMe().length() > 0 ? request.getAboutMe() : null);
			// nếu có password mới, thay đổi
			if (request.getNewPassword().length() > 0)
				user.setPassword(passwordEncoder.encode(request.getNewPassword()));

			// lưu người dùng
			userRepository.save(user);
			return "redirect:/profile?userID=" + user.getId();
		} else {
			model.addAttribute("error", "Incorrect Password!");
			return "/profile";
		}

	}

	@PostMapping("/createAlbum")
	public String createAlbum(Model model, @RequestParam("newAlbum") String newAlbum) {

		// Tìm kiếm album theo tên
		Album album = albumRepository.findByName(newAlbum).orElse(null);

		// Nếu đã tồn tại, báo lỗi
		if (album != null) {
			model.addAttribute("warn", "This album exist");
			return profilePage(null, model);
		} else {
			model = setModel(model);
			User user = getUser(model);
			// tạo album
			album = new Album(newAlbum, user);
			// lưu vào database
			albumRepository.save(album);
			model.addAttribute("success", "Create new album success!");
		}
		return profilePage(null, model);
	}

	@SuppressWarnings("unused")
	@PostMapping("/upload")
	public String uploadImage(@ModelAttribute("createPostRequest") CreatePostRequest request, Model model) {
		model = setModel(model);

		// tạo các biến kiểm tra
		// totalFile: số lượng file sẽ được upload
		// successCount số file upload thành công
		int successCount = 0, totalFile = request.getFiles().length;

		User user = getUser(model);
		Album album = null;
		// lấy thông tin album
		if (request.getAlbumID() != null)
			album = albumRepository.findById(request.getAlbumID()).orElse(null);
		// tạo post
		Post post = new Post(request.getTitle(), request.getContent(), request.getPrice(), null, user);
		postRepository.save(post);
		post = postRepository.findLastest().get(0);

		String tagList[] = request.getTag().split(" ");
		for (String tagName : tagList) {
			if (tagName.length() > 0) {
				Tag tag = tagRepository.findByName(tagName).orElse(null);
				if (tag == null) {
					tag = new Tag(null, tagName, null);
					tagRepository.save(tag);
				}
				postTagRepository.save(new PostTag(null, post, tag));
			}
		}

		// tạo danh sách image của post
		Set<Image> imageSet = new HashSet<Image>();
		// lưu post
		postRepository.save(post);

		// nhận list image upload từ máy tính
		for (MultipartFile file : request.getFiles()) {
			// nếu là file ảnh jpg/png
			if (file.getOriginalFilename().contains(".jpg") || file.getOriginalFilename().contains("png"))
				try {
					// tạo file tạm lưu tại C:\Users\PC\AppData\Local\Temp
					File uploadFile = new File(System.getProperty("java.io.tmpdir") + "/" + file.getOriginalFilename());
					file.transferTo(uploadFile);
					// upload ảnh có tên ??? tại địa chỉ ??? với định dạng image/jpg lên driver
					com.google.api.services.drive.model.File fileUpload = driveService.uploadFile(uploadFile.getName(),
							uploadFile.getAbsolutePath(), "image/jpg");
					// đánh dấu thêm 1 file upload thành công
					successCount += 1;
					// lấy link ảnh từ driver
					String imageLink = "https://drive.google.com/uc?export=view&id=" + fileUpload.getId();
					// tạo image
					Image image = new Image(fileUpload.getId(), imageLink, album, post);
					// lưu image vào database
					imageRepository.save(image);
					// thêm image vào danh sách của post
					imageSet.add(image);
				} catch (Exception e) {
					e.printStackTrace();
				}
		}

		// nếu số lượng ảnh được upload thành công cao hơn 0
		if (successCount > 0) {
			// thông báo thành công success/total
			model.addAttribute("success",
					("Upload " + String.valueOf(successCount) + "/" + String.valueOf(totalFile) + " file success!"));
			// gán danh sách ảnh vào post
			post.setImages(imageSet);
			// lưu post vào database
			postRepository.save(post);

			// đặt chế độ follow cho người viết bài
			setFollow(getUser(model), post);
		} else {
			// nếu số lượng ảnh upload thành công = 0
			// xóa post
			postRepository.delete(post);
		}
		int failCount = totalFile - successCount;
		// nếu số lượng file lỗi lớn hơn 0
		// báo lỗi
		if (failCount > 0) {
			model.addAttribute("error", ("Upload " + String.valueOf(failCount) + "/" + String.valueOf(totalFile)
					+ " file fail because of invalid type!"));
		}

		return homePage(model, null);
	}

	@GetMapping("/post")
	public String postPage(@RequestParam(name = "postID") Long postID, Model model) {
		model = setModel(model);

		// lấy thông tin post từ database theo id
		Post post = postRepository.findById(postID).orElse(null);

		if (post == null) {
			model.addAttribute("error", "Post not exist");
			return homePage(model, null);
		}

		List<Tag> tagList = tagRepository.findByPostID(postID);
		model.addAttribute("tagList", tagList);

		Image image = imageRepository.findByPostID(postID).get(0);
		model.addAttribute("album", image.getAlbum());

		User user = getUser(model);
		if (user != null) {
			List<Follow> followList = followRepository.findByPostID_UserID(post.getId(), user.getId());
			if (followList.size() > 0)
				model.addAttribute("isFollow", true);
			else
				model.addAttribute("isFollow", false);

			List<LikeCount> likeList = likeRepository.findByPostID_UserID(postID, user.getId());
			if (likeList.size() > 0)
				model.addAttribute("isLike", true);
			else
				model.addAttribute("isLike", false);

		}

		// lấy danh sách comment của post bởi postID
		List<Comment> commentList = commentRepository.findByPostID(postID);

		model.addAttribute("post", post);
		model.addAttribute("commentList", commentList);

		return "post";
	}

	// lấy danh sách ảnh của post
	@GetMapping("/getImageList")
	@ResponseBody
	public List<String> getImageList(@RequestParam("postID") Long postID) {

		List<Image> imageList = imageRepository.findByPostID(postID);

		List<String> rs = new ArrayList<String>();
		for (Image image : imageList) {
			rs.add(image.getLink());
		}

		return rs;

	}

	// chỉnh sửa thông tin post
	@PostMapping("/editPost")
	public String editPost(@ModelAttribute("post") Post request, Model model) {
		model = setModel(model);

		// tìm kiếm post theo id
		Post post = postRepository.findById(request.getId()).orElse(null);
		// nếu post không tồn tại => báo lỗi
		if (post == null) {
			model.addAttribute("error", "Post not exist");
			return homePage(model, null);
		}

		// update
		post.setTitle(request.getTitle());
		post.setContent(request.getContent());
		post.setPrice(request.getPrice());
		// lưu vào database
		postRepository.save(post);

		model.addAttribute("success", "Edit post success!");

		return postPage(request.getId(), model);
	}

	// xóa post
	@PostMapping("/deletePost")
	public String deletePost(@ModelAttribute("post") Post request, Model model) {
		model = setModel(model);

		// lấy thông tin post từ database theo id
		Post post = postRepository.findById(request.getId()).orElse(null);
		// xóa post khỏi database
		postRepository.delete(post);

		model.addAttribute("success", "Delete success");

		return homePage(model, null);
	}

	// tạo comment
	@PostMapping("/newComment")
	public String createComment(@RequestParam("newComment-postID") String postID,
			@RequestParam("newComment-content") String content, Model model) {

		model = setModel(model);

		User user = getUser(model);
		// lấy thông tin post từ database
		Post post = postRepository.findById(Long.valueOf(postID)).orElse(null);
		// tạo comment
		Comment comment = new Comment(null, content, post, user);
		// lưu comment vào database
		commentRepository.save(comment);

		// kiểm tra người comment đã follow post chưa?
		// nếu chưa, set follow
		if (followRepository.findByPostID_UserID(post.getId(), user.getId()).size() == 0)
			setFollow(user, post);

		// lấy danh sách follow post này
		List<Follow> followList = followRepository.findByPostID(post.getId());
		for (Follow follow : followList) {
			// ngoại trừ người đã comment, tạo thông báo đến tất cả những người đã follow
			// khác
			if (follow.getUser() != user) {
				notifyRepository.save(new Notify(null, "New comment", post, follow.getUser()));
			}
		}

		return postPage(post.getId(), model);
	}

	// report bài viết
	@PostMapping("report")
	public String report(@ModelAttribute("post") Post request, Model model) {
		model = setModel(model);

		// tìm kiếm bài post từ database
		Post post = postRepository.findById(request.getId()).orElse(null);
		// nếu không có => trả về home
		if (post == null)
			return homePage(model, null);

		// lấy danh sách tài khoản có role admin
		List<User> adminList = userRepository.findByRoleID(Long.valueOf(2));
		for (User user : adminList) {
			// tạo notify cho admin
			notifyRepository.save(new Notify(null, "Report", post, user));
		}

		return homePage(model, null);
	}

	@PostMapping("/follow")
	public String follow(@ModelAttribute("post") Post request, Model model) {
		model = setModel(model);
		User user = getUser(model);
		Post post = postRepository.findById(request.getId()).orElse(null);
		if (post == null) {
			model.addAttribute("error", "Post not exist");
			return homePage(model, null);
		}
		setFollow(user, post);
		return postPage(post.getId(), model);
	}

	@PostMapping("/unfollow")
	public String unfollow(@ModelAttribute("post") Post request, Model model) {
		model = setModel(model);
		User user = getUser(model);
		Post post = postRepository.findById(request.getId()).orElse(null);
		if (post == null) {
			model.addAttribute("error", "Post not exist");
			return homePage(model, null);
		}

		Follow follow = followRepository.findByPostID_UserID(post.getId(), user.getId()).get(0);
		if (follow != null)
			followRepository.delete(follow);
		return postPage(post.getId(), model);
	}

	@PostMapping("/like")
	public String like(@ModelAttribute("post") Post request, Model model) {
		model = setModel(model);
		User user = getUser(model);
		Post post = postRepository.findById(request.getId()).orElse(null);
		if (post == null) {
			model.addAttribute("error", "Post not exist");
			return homePage(model, null);
		}
		LikeCount like = new LikeCount(null, user, post);
		likeRepository.save(like);
		model.addAttribute("totalLike", likeRepository.findAll().size());
		return postPage(post.getId(), model);
	}

	@PostMapping("/unlike")
	public String unlike(@ModelAttribute("post") Post request, Model model) {
		model = setModel(model);
		User user = getUser(model);
		Post post = postRepository.findById(request.getId()).orElse(null);
		if (post == null) {
			model.addAttribute("error", "Post not exist");
			return homePage(model, null);
		}
		LikeCount like = likeRepository.findByPostID_UserID(post.getId(), user.getId()).get(0);
		if (like != null)
			likeRepository.delete(like);
		model.addAttribute("totalLike", likeRepository.findAll().size());
		return postPage(post.getId(), model);
	}

	// xóa bài viết
	@PostMapping("/deleteByReport")
	public String lock(@ModelAttribute("post") Post request, Model model) {
		model = setModel(model);
		Post post = postRepository.findById(request.getId()).orElse(null);
		if (post != null)
			postRepository.delete(post);
		return homePage(model, null);
	}
}