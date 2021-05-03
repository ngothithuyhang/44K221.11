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

	// lấy danh sách thông báo theo userID
	private List<Notify> getNotify(User user, int limit) {
		if (user == null)
			return null;
		List<Notify> notifyList = notifyRepository.findByUserIDStatus(user.getId(), 0, limit);
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
			model.addAttribute("currentUser", user);
			Role role = roleRepository.findByUserID(user.getId()).get(0);
			model.addAttribute("role", role.getId());

			// Get list album from database
			List<Album> albumList = albumRepository.findByUserID(user.getId());
			model.addAttribute("albumList", albumList);

			// Get list notify from database
			notifyList = getNotify(user, 10);

		}

		model.addAttribute("notifyList", notifyList);

		return model;

	}

	// Thêm 1 liên hệ follow vào bài viết
	private void setFollow(User user, Post post) {
		Follow follow = new Follow(null, post, user);
		followRepository.save(follow);
	}

	// Trả về trang home
	@GetMapping(value = { "", "/", "/home" })
	public String homePage(Model model, @RequestParam(value = "search", required = false) String search) {
		model = setModel(model);

		List<Post> postList = new ArrayList<Post>();
		List<PostImage> postImageList = new ArrayList<PostImage>();

		// Kiểm tra danh sách từ khóa filter
		@SuppressWarnings("unchecked")
		Set<String> keySet = (Set<String>) model.getAttribute("keySet");

		// Nếu có từ khóa => Lọc theo từ khóa
		if (keySet != null) {
			List<Tag> tagList = new ArrayList<Tag>();
			for (String key : keySet) {
				Tag tag = tagRepository.findByName(key).orElse(null);
				if (tag != null)
					tagList.add(tag);
			}

			// select postTag.* from postTag inner join tag on postTag.tag_id = tag.id where
			// tag.name=??? group by postTag.post_id
			Specification<PostTag> specification = Specification.where(hasTagIn(tagList));
			GenericSpecifications<PostTag> genericSpecifications = new GenericSpecifications<PostTag>();
			List<String> joinColumn = new ArrayList<String>();
			joinColumn.add("post");

			specification = genericSpecifications.groupBy(specification, joinColumn);

			// lấy 8 bài viết khớp từ khóa gần nhất
			Page<PostTag> listPostTag = postTagRepository.findAll(specification, PageRequest.of(0, 8));
			for (PostTag postTag : listPostTag) {
				postList.add(postTag.getPost());
			}

		}
		// nếu không chứa từ khóa filter
		else {
			// nếu tìm kiếm bởi tiêu đề
			if (search != null) {
				// cắt nhỏ tiêu đề cần tìm thành các từ đơn
				String[] key = search.split(" ");
				search = "%";
				// tạo query lệnh like trong sql => tìm kiếm theo từ
				for (String string : key) {
					if (string.length() > 0) {
						search += string + "%";
					}
				}
			} else
				// Nếu không phải tìm theo tiêu đề, tiến hành tìm kiếm bình thường
				search = new String("%");

			// Trả về 8 bài viết thích hợp gần nhất
			postList = postRepository.findTop(search, 8);

		}
		Random rd = new Random();

		// Với mỗi bài viết tìm được
		for (Post post : postList) {
			// Lấy danh sách ảnh của bài viết
			List<Image> imageList = imageRepository.findByPostID(post.getId());

			// Nếu nhiều hơn 0 ảnh
			if (imageList.size() > 0) {
				// tạo thumbnail bài viết từ postID và 1 ảnh random từ bài viết
				PostImage postImage = new PostImage(post.getId(),
						imageList.get(rd.nextInt(imageList.size())).getLink());
				postImageList.add(postImage);
			}
		}

		// Lưu lại danh sách thumbnail bài viết
		model.addAttribute("postImageList", postImageList);

		return "home";
	}

	// Kiểm tra từ khóa từ filter
	// Nếu có từ khóa => lưu lại
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

		// Chuyển về home
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
		List<Album> albumList = albumRepository.findByUserID(user.getId());
		// Lưu lại danh sách album
		model.addAttribute("albumList", albumList);

		// Tạo danh sách thumbnail bài viết
		List<PostImage> postImageList = new ArrayList<PostImage>();
		// Lấy danh sách bài viết từ database
		List<Post> postList = postRepository.findByUserID(userID);

		Random rd = new Random();
		// Với mỗi bài viết
		for (Post post : postList) {
			List<Image> imageList = imageRepository.findByPostID(post.getId());

			if (imageList.size() > 0) {
				// Tạo thumbnail từ postID và 1 ảnh random
				PostImage postImage = new PostImage(post.getId(),
						imageList.get(rd.nextInt(imageList.size())).getLink());
				postImageList.add(postImage);
			}
		}

		// Lưu danh sách thumbnail
		model.addAttribute("postImageList", postImageList);

		// Tạo request chỉnh sửa thông tin người dùng
		// Không chắc người dùng sẽ sử dụng nhưng nếu không tạo thì trang sẽ bị lỗi
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

			HttpSession session = httpSessionFactory.getObject();
			session.setAttribute("accessToken", accessToken);

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

		// tách nhỏ dãy tag thành từng tag đơn
		String tagList[] = request.getTag().split(" ");
		// với mỗi tag
		for (String tagName : tagList) {
			if (tagName.length() > 0) {
				// Kiểm tra xem tag có tên ??? đã tồn tại trong database chưa
				Tag tag = tagRepository.findByName(tagName).orElse(null);
				// Nếu chưa => tạo mới
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
					com.google.api.services.drive.model.File fileUpload = driveService.uploadFile(file, "image/jpg");
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
			
			notifyRepository.save(new Notify(null,"Your post created! It will be public after accepted by admin!",post,user));
			List<User> adminList = userRepository.findByRoleID(Long.valueOf(2));
			for (User admin : adminList) {
				notifyRepository.save(new Notify(null, "New post waiting for accept!", post, admin));
			}
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

		// Nếu không tìm thấy post => báo lỗi không tồn tại
		if (post == null) {
			model.addAttribute("error", "Post not exist");
			return homePage(model, null);
		}

		// Lấy danh sách tag của bài viết
		List<Tag> tagList = tagRepository.findByPostID(postID);
		// Lưu lại
		model.addAttribute("tagList", tagList);

		// Lấy danh sách ảnh của bài viết
		Image image = imageRepository.findByPostID(postID).get(0);
		// Lưu lại
		model.addAttribute("album", image.getAlbum());

		// Lấy thông tin người dùng hiện tại
		User user = getUser(model);
		// Nếu có thông tin => đã đăng nhập
		if (user != null) {
			// Kiểm tra xem có tồn tại bản ghi follow của người dùng ??? tại bài viết ???
			// hay không
			List<Follow> followList = followRepository.findByPostID_UserID(post.getId(), user.getId());
			// Nếu có => lưu lại đã theo dõi => mở khóa nút hủy theo dõi
			if (followList.size() > 0)
				model.addAttribute("isFollow", true);
			else
				// nếu chưa => lưu chưa theo dõi => mở kháo nút theo dõi
				model.addAttribute("isFollow", false);

			// Kiểm tra xem người dùng ??? đã like post chưa
			List<LikeCount> likeList = likeRepository.findByPostID_UserID(postID, user.getId());
			if (likeList.size() > 0)
				// rồi => cho phép unlike
				model.addAttribute("isLike", true);
			else
				// chưa => cho phép like
				model.addAttribute("isLike", false);

		}

		// lấy danh sách comment của post bởi postID
		List<Comment> commentList = commentRepository.findByPostID(postID);

		model.addAttribute("post", post);
		model.addAttribute("commentList", commentList);

		if (user != null) {
			Set<Role> roleSet = user.getRoles();
			for (Role role : roleSet) {
				if (role.getName() == RoleName.ROLE_ADMIN)
					model.addAttribute("isAdmin", true);
			}
		}

		System.out.println(model.getAttribute("isAdmin"));
		
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

	// Theo dõi bài viết
	@PostMapping("/follow")
	public String follow(@ModelAttribute("post") Post request, Model model) {
		model = setModel(model);
		User user = getUser(model);

		// Tìm kiếm bài viết theo id
		Post post = postRepository.findById(request.getId()).orElse(null);
		if (post == null) {
			model.addAttribute("error", "Post not exist");
			return homePage(model, null);
		}
		// Đặt follow cho người dùng ??? tại bài viết ???
		setFollow(user, post);
		return postPage(post.getId(), model);
	}

	// Hủy theo dõi
	@PostMapping("/unfollow")
	public String unfollow(@ModelAttribute("post") Post request, Model model) {
		model = setModel(model);
		// Lấy thông tin người dùng hiện tại
		User user = getUser(model);
		// Lấy thông tin bài viết
		Post post = postRepository.findById(request.getId()).orElse(null);
		if (post == null) {
			model.addAttribute("error", "Post not exist");
			return homePage(model, null);
		}

		// Lấy thông tin follow
		Follow follow = followRepository.findByPostID_UserID(post.getId(), user.getId()).get(0);
		if (follow != null)
			// xóa follow
			followRepository.delete(follow);
		return postPage(post.getId(), model);
	}

	// Like bài viết
	@PostMapping("/like")
	public String like(@ModelAttribute("post") Post request, Model model) {
		model = setModel(model);
		// lấy thông tin người dùng
		User user = getUser(model);
		// lấy thông tin bài viết
		Post post = postRepository.findById(request.getId()).orElse(null);
		if (post == null) {
			model.addAttribute("error", "Post not exist");
			return homePage(model, null);
		}

		// Thêm một lượt like
		LikeCount like = new LikeCount(null, user, post);
		likeRepository.save(like);
		model.addAttribute("totalLike", likeRepository.findAll().size());
		return postPage(post.getId(), model);
	}

	// unlike bài viết
	@PostMapping("/unlike")
	public String unlike(@ModelAttribute("post") Post request, Model model) {
		model = setModel(model);
		// lấy thông tin người dùng
		User user = getUser(model);
		// lấy thông tin bài viết
		Post post = postRepository.findById(request.getId()).orElse(null);
		if (post == null) {
			model.addAttribute("error", "Post not exist");
			return homePage(model, null);
		}
		// Xóa một lượt like
		LikeCount like = likeRepository.findByPostID_UserID(post.getId(), user.getId()).get(0);
		if (like != null)
			likeRepository.delete(like);
		model.addAttribute("totalLike", likeRepository.findAll().size());
		return postPage(post.getId(), model);
	}

	// xóa bài viết
	@GetMapping("/lockPost")
	public String lock(@RequestParam("postID")Long postID, Model model) {
		model = setModel(model);
		// Lấy thông tin bài viết
		Post post = postRepository.findById(postID).orElse(null);
		if (post != null && post.getStatus()==1) {
			post.setStatus(0);
			postRepository.save(post);
			notifyRepository.save(new Notify(null, "Your post has been locked by admin!", post, post.getUser()));
		}
		return postPage(post.getId(), model);

	}
	
	@GetMapping("/acceptPost")
	public String acceptPost(@RequestParam("postID")Long postID, Model model) {
		model = setModel(model);
		// Lấy thông tin bài viết
		Post post = postRepository.findById(postID).orElse(null);
		if (post != null && post.getStatus()==0) {
			post.setStatus(1);
			postRepository.save(post);
			notifyRepository.save(new Notify(null, "Your post has been accepted by admin!", post, post.getUser()));
		}
		return postPage(post.getId(), model);
	}
	
	@GetMapping("readNotify")
	public String readNotify(Model model, @RequestParam("notifyID")Long notifyID) {
		
		User currentUser = getUser(model);
		if(currentUser==null)
			return homePage(model, null);
		
		Notify notify = notifyRepository.findById(notifyID).orElse(null);
		if(notify==null || currentUser!=notify.getUser())
			return homePage(model, null);
		
		if(notify.getStatus()==0) {
			notify.setStatus(1);
			notifyRepository.save(notify);
		}
		
		return postPage(notify.getPost().getId(), model);
	}
	
	@GetMapping("deleteNotify")
	public String deleteNotify(Model model, @RequestParam("notifyID")Long notifyID) {
		
		User currentUser = getUser(model);
		if(currentUser==null)
			return homePage(model, null);
		
		Notify notify = notifyRepository.findById(notifyID).orElse(null);
		if(notify==null|| currentUser!=notify.getUser())
			return homePage(model, null);
		
		notifyRepository.delete(notify);
		model.addAttribute("success", "Delete notify success");
		return notifyList(model);
	}
	
	@GetMapping("notifyList")
	public String notifyList(Model model) {
		model = setModel(model);
		User currentUser = getUser(model);
		if(currentUser==null)
			return homePage(model, null);
		
		List<Notify> notifyList = notifyRepository.findByUserID(currentUser.getId(), 20);
		model.addAttribute("notifyList", notifyList);
		return "notify";
	}

}