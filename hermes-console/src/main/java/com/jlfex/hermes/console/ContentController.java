package com.jlfex.hermes.console;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.jlfex.hermes.common.Logger;
import com.jlfex.hermes.model.Article;
import com.jlfex.hermes.model.ArticleCategory;
import com.jlfex.hermes.model.ImageManage;
import com.jlfex.hermes.model.TmpNotice;
import com.jlfex.hermes.service.ContentService;
import com.jlfex.hermes.service.pojo.ContentCategory;
import com.jlfex.hermes.service.pojo.FriendLinkVo;
import com.jlfex.hermes.service.pojo.PublishContentVo;
import com.jlfex.hermes.service.pojo.ResultVo;
import com.jlfex.hermes.service.pojo.TmpNoticeVo;

/**
 * @author admin 内容管理控制器
 */
@Controller
@RequestMapping("/content")
public class ContentController {
	@Autowired
	private ContentService contentService;

	@InitBinder
	public void initBinder(WebDataBinder binder) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		dateFormat.setLenient(true);
		binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, true));
	}

	/**
	 * 分类管理结果页面
	 * 
	 */
	@RequestMapping("/categoryIndex")
	public String categoryIndex(@RequestParam(value = "page", defaultValue = "0") Integer page, @RequestParam(value = "size", defaultValue = "10") Integer size, Model model) {
		model.addAttribute("categories", contentService.findByLevelNotNull(page, size));
		return "/content/categoryIndex";
	}

	/**
	 * 删除分类
	 * 
	 * @author lishunfeng
	 */
	@RequestMapping("/deleteCategory")
	@ResponseBody
	public ResultVo deleteCategory(@RequestParam(value = "id", required = true) String id, Model model) {
		return contentService.deleteCategory(id);
	}

	/**
	 * 跳转至编辑分类页面
	 * 
	 * @author lishunfeng
	 */
	@RequestMapping("/editCategory")
	public String editCategory(@RequestParam(value = "id", required = true) String id, Model model) {
		model.addAttribute("category", contentService.findOne(id));
		ArticleCategory levelThree = null, levelTwo = null, levelOne = null;
		ArticleCategory temp = contentService.findOne(id);
		if (temp != null) {
			if (("三级").equals(temp.getLevel())) {
				levelThree = temp;
			}
			if (("二级").equals(temp.getLevel())) {
				levelTwo = temp;
			}
		}
		// 如果三级不为空，获得二级分类，并给二级赋值
		if (levelThree != null) {
			levelTwo = levelThree.getParent();
			model.addAttribute("levelTwo", levelTwo.getId());
		}
		// 如果二级不为空，获得一级分类，并给一级赋值
		if (levelTwo != null) {
			levelOne = levelTwo.getParent();
			model.addAttribute("levelOne", levelOne.getId());
		}
		// 如果一级不为空，查询出二级，并赋值
		if (levelOne != null) {
			model.addAttribute("categoryForLevel2", contentService.findByParentId(levelOne.getId()));
		}
		model.addAttribute("categoryForLevel1", contentService.findCategoryByLevel("一级"));

		return "/content/editCategory";

	}

	/**
	 * 处理编辑分类逻辑
	 * 
	 * @author lishunfeng
	 */
	@RequestMapping("/updateCategory")
	public String updateCategory(ContentCategory contentCategory, RedirectAttributes attr) {
		try {
			contentService.updateCategory(contentCategory);
			attr.addFlashAttribute("msg", "修改分类成功");
			return "redirect:/content/categoryIndex";
		} catch (Exception e) {
			attr.addFlashAttribute("msg", "修改分类失败");
			Logger.error("修改分类失败：", e);
			return "redirect:/content/updateCategory";
		}
	}

	/**
	 * 跳转至新增分类页面
	 * 
	 * @author lishunfeng
	 */
	@RequestMapping("/addCategory")
	public String addCategory(Model model) {
		return "/content/addCategory";
	}

	/**
	 * 处理新增分类逻辑
	 * 
	 * @author lishunfeng
	 */
	@RequestMapping("/insertCategory")
	public String insertCategory(ContentCategory contentCategory, RedirectAttributes attr) {
		try {
			if (StringUtils.isEmpty(contentCategory.getCategoryLevelOne())) {
				attr.addFlashAttribute("msg", "您还未选择任何分类");
			} else {
				contentService.insertCategory(contentCategory);
				attr.addFlashAttribute("msg", "新增分类成功");
			}
			return "redirect:/content/addCategory";
		} catch (Exception e) {
			attr.addFlashAttribute("msg", "新增分类失败");
			Logger.error("新增分类失败：", e);
			return "redirect:/content/addCategory";
		}
	}

	/**
	 * 根据类型查找分类
	 * 
	 * @author lishunfeng
	 */
	@RequestMapping("/findCategoryByLevel")
	@ResponseBody
	public List<ArticleCategory> findCategoryByLevel(@RequestParam("level") String level) {
		return contentService.findCategoryByLevel(level);
	}

	/**
	 * 根据父类查找子类
	 * 
	 * @author lishunfeng
	 */
	@RequestMapping("/findCategoryByParent")
	@ResponseBody
	public List<ArticleCategory> findCategoryByParent(@RequestParam("level") String level, @RequestParam("parentCode") String parentCode, @RequestParam(required = true) String parentId) {
		return contentService.findByParentId(parentId);
	}

	/**
	 * 内容管理首页
	 * 
	 * @author lishunfeng
	 */
	@RequestMapping("/contentIndex")
	public String index(String levelOne, String levelTwo, Model model) {
		model.addAttribute("categoryForLevel1", contentService.findCategoryByLevel("一级"));
		model.addAttribute("categoryForLevel2", contentService.findByParentId(levelOne));
		model.addAttribute("categoryForLevel3", contentService.findByParentId(levelTwo));
		return "/content/contentIndex";
	}

	/**
	 * 内容管理查询数据结果页面
	 * 
	 * @author lishunfeng
	 */
	@RequestMapping("/contentData")
	public String productData(String levelOne, String levelTwo, String levelThree, String inputName, @RequestParam(value = "page", defaultValue = "0") Integer page, @RequestParam(value = "size", defaultValue = "10") Integer size, Model model) {
		model.addAttribute("contentData", contentService.find(levelOne, levelTwo, levelThree, inputName, page, size));
		return "/content/contentData";
	}

	/**
	 * 发布内容页面
	 * 
	 * @author lishunfeng
	 */
	@RequestMapping("/publish")
	public String publish(String levelOne, String levelTwo, Model model) {
		model.addAttribute("categoryForLevel1", contentService.findCategoryByLevel("一级"));
		model.addAttribute("categoryForLevel2", contentService.findByParentId(levelOne));
		model.addAttribute("categoryForLevel3", contentService.findByParentId(levelTwo));
		return "/content/addPublish";
	}

	/**
	 * 点击发布页面提交按钮处理逻辑
	 * 
	 * @author lishunfeng
	 */
	@RequestMapping("/addPublish")
	public String addPublish(PublishContentVo pcVo, RedirectAttributes attr, Model model) {
		try {
			contentService.addPublish(pcVo);
			attr.addFlashAttribute("msg", "发布内容成功");
			return "redirect:/content/contentIndex";
		} catch (Exception e) {
			attr.addFlashAttribute("msg", "发布内容失败");
			Logger.error("发布内容失败：", e);
			return "redirect:/content/addPublish";
		}
	}

	/**
	 * 提交并继续发布处理逻辑
	 * 
	 * @author lishunfeng
	 */
	@RequestMapping("/addPublishAgain")
	public String addPublishAgain(PublishContentVo pcVo, RedirectAttributes attr, Model model) {
		try {
			contentService.addPublish(pcVo);
			attr.addFlashAttribute("msg", "发布内容成功");
		} catch (Exception e) {
			attr.addFlashAttribute("msg", "发布内容失败");
			Logger.error("发布内容失败：", e);
		}
		return "redirect:/content/publish";
	}

	/**
	 * 编辑内容页面
	 * 
	 * @author lishunfeng
	 */
	@RequestMapping("/editContent")
	public String editContent(@RequestParam(value = "id", required = true) String id, Model model) {
		model.addAttribute("article", contentService.findOneById(id));
		Article article = contentService.findOneById(id);
		ArticleCategory levelThree = null, levelTwo = null, levelOne = null;
		ArticleCategory temp = article.getCategory();
		if (temp != null) {
			if (("三级").equals(temp.getLevel())) {
				levelThree = temp;
			}
			if (("二级").equals(temp.getLevel())) {
				levelTwo = temp;
			}
			if (("一级").equals(temp.getLevel())) {
				levelOne = temp;
			}
		}
		if (levelThree != null) {
			model.addAttribute("levelThree", levelThree.getId());
			levelTwo = levelThree.getParent();
		}
		// 三级
		if (levelTwo != null) {
			model.addAttribute("categoryForLevel3", contentService.findByParentId(levelTwo.getId()));
			model.addAttribute("levelTwo", levelTwo.getId());
			levelOne = levelTwo.getParent();
		}
		// 二级
		if (levelOne != null) {
			model.addAttribute("categoryForLevel2", contentService.findByParentId(levelOne.getId()));
			model.addAttribute("levelOne", levelOne.getId());
		}
		// 一级
		model.addAttribute("categoryForLevel1", contentService.findCategoryByLevel("一级"));

		return "/content/editContent";
	}

	/**
	 * 点击编辑内容页面保存按钮处理逻辑
	 * 
	 * @author lishunfeng
	 */
	@RequestMapping("/handerEditContent")
	public String handerEditContent(PublishContentVo pcVo, RedirectAttributes attr, Model model) {
		try {
			contentService.updateContent(pcVo);
			attr.addFlashAttribute("msg", "内容修改成功");
			return "redirect:/content/contentIndex";
		} catch (Exception e) {
			attr.addFlashAttribute("msg", "内容修改失败");
			Logger.error("编辑内容失败：", e);
			return "redirect:/content/editContent";
		}
	}

	/**
	 * 删除内容
	 * 
	 * @author lishunfeng
	 */
	@RequestMapping("/deleteContent")
	public String deleteContent(@RequestParam(value = "id", required = true) String id, RedirectAttributes attr, Model model) {
		try {
			contentService.deleteContent(id);
			attr.addFlashAttribute("msg", "删除内容成功");
			return "redirect:/content/contentIndex";
		} catch (Exception e) {
			attr.addFlashAttribute("msg", "删除内容失败");
			Logger.error("删除内容失败：", e);
			return "redirect:/content/contentIndex";
		}
	}

	/**
	 * 批量删除内容
	 * 
	 * @author lishunfeng
	 */

	@RequestMapping("/batchDeleteContent")
	public String batchDeleteContent(@RequestParam(value = "ids", required = true) String ids, RedirectAttributes attr, Model model) {
		try {
			contentService.batchDeleteContent(ids);
			attr.addFlashAttribute("msg", "批量删除成功");
			return "redirect:/content/contentIndex";
		} catch (Exception e) {
			attr.addFlashAttribute("msg", "批量删除失败");
			Logger.error("批量删除内容失败：", e);
			return "redirect:/content/contentIndex";
		}
	}

	/**
	 * 友情链接结果页面
	 * 
	 * @author lishunfeng
	 */
	@RequestMapping("/friendLink")
	public String friendLinkData(@RequestParam(value = "page", defaultValue = "0") Integer page, @RequestParam(value = "size", defaultValue = "10") Integer size, Model model) {
		model.addAttribute("friendLinks", contentService.findAll(page, size));
		return "/content/friendLinkIndex";
	}

	/**
	 * 新增友情链接
	 * 
	 * @author lishunfeng
	 */
	@RequestMapping("/addFriendLink")
	public String addFriendLink(Model model) {
		return "/content/addFriendLink";
	}

	/**
	 * 点击添加链接页面保存按钮处理逻辑
	 * 
	 * @author lishunfeng
	 */
	@RequestMapping("/handerAddFriendLink")
	public String handerAddFriendLink(FriendLinkVo flVo, RedirectAttributes attr, Model model) {
		try {
			contentService.addFriendLink(flVo);
			attr.addFlashAttribute("msg", "添加链接成功");
			return "redirect:/content/friendLink";
		} catch (Exception e) {
			attr.addFlashAttribute("msg", "添加链接失败");
			Logger.error("添加链接失败：", e);
			return "redirect:/content/addFriendLink";
		}
	}

	/**
	 * 编辑友情链接页面
	 * 
	 * @author lishunfeng
	 */
	@RequestMapping("/editFriendLink")
	public String editFriendLink(@RequestParam(value = "id", required = true) String id, Model model) {
		model.addAttribute("friendLink", contentService.findOneBy(id));
		return "/content/editFriendLink";
	}

	/**
	 * 点击编辑友情链接页面保存按钮处理逻辑
	 * 
	 * @author lishunfeng
	 */
	@RequestMapping("/handerEditFriendLink")
	public String handerEditFriendLink(FriendLinkVo flVo, RedirectAttributes attr, Model model) {
		try {
			contentService.updateFriendLink(flVo);
			attr.addFlashAttribute("msg", "友情链接修改成功");
			return "redirect:/content/friendLink";
		} catch (Exception e) {
			attr.addFlashAttribute("msg", "友情链接修改失败");
			Logger.error("友情链接修改失败：", e);
			return "redirect:/content/editFriendLink";
		}
	}

	/**
	 * 删除友情链接
	 * 
	 * @author lishunfeng
	 */
	@RequestMapping("/deleteFriendLink")
	public String deleteFriendLink(@RequestParam(value = "id", required = true) String id, RedirectAttributes attr, Model model) {
		try {
			contentService.deleteFriendLink(id);
			attr.addFlashAttribute("msg", "删除友情链接成功");
			return "redirect:/content/friendLink";
		} catch (Exception e) {
			attr.addFlashAttribute("msg", "删除友情链接失败");
			Logger.error("删除友情链接失败：", e);
			return "redirect:/content/friendLink";
		}
	}

	/**
	 * 临时公告结果页面
	 * 
	 * @author lishunfeng
	 */
	@RequestMapping("/tmpNotice")
	public String tmpNoticeData(@RequestParam(value = "page", defaultValue = "0") Integer page, @RequestParam(value = "size", defaultValue = "10") Integer size, Model model) {
		model.addAttribute("tmpNotices", contentService.findAllTmpNotices(page, size));
		return "/content/tmpNoticeIndex";
	}

	/**
	 * 新增临时公告
	 * 
	 * @author lishunfeng
	 */
	@RequestMapping("/addTmpNotice")
	public String addTmpNotice(Model model) {
		return "/content/addTmpNotice";
	}

	/**
	 * 新增公告处理逻辑
	 * 
	 * @author lishunfeng
	 */
	@RequestMapping("/handerAddTmpNotice")
	public String handerAddTmpNotice(TmpNotice tmpNotice, RedirectAttributes attr, Model model) {
		try {
			contentService.addTmpNotice(tmpNotice);
			attr.addFlashAttribute("msg", "添加公告成功");
			return "redirect:/content/tmpNotice";
		} catch (Exception e) {
			attr.addFlashAttribute("msg", "添加公告失败");
			Logger.error("添加公告失败：", e);
			return "redirect:/content/addTmpNotice";
		}
	}

	/**
	 * 编辑临时公告页面
	 * 
	 * @author lishunfeng
	 */
	@RequestMapping("/editTmpNotice")
	public String editTmpNotice(@RequestParam(value = "id", required = true) String id, Model model) {
		model.addAttribute("tmpNotice", contentService.findOneByTmpNoticeId(id));
		return "/content/editTmpNotice";
	}

	/**
	 * 编辑临时公告处理逻辑
	 * 
	 * @author lishunfeng
	 */
	@RequestMapping("/handerEditTmpNotice")
	public String handerEditTmpNotice(TmpNoticeVo tnVo, RedirectAttributes attr, Model model) {
		try {
			contentService.updateTmpNotice(tnVo);
			attr.addFlashAttribute("msg", "临时公告修改成功");
			return "redirect:/content/tmpNotice";
		} catch (Exception e) {
			attr.addFlashAttribute("msg", "临时公告修改失败");
			Logger.error("临时公告修改失败：", e);
			return "redirect:/content/editTmpNotice";
		}
	}

	/**
	 * 删除临时公告
	 * 
	 * @author lishunfeng
	 */
	@RequestMapping("/deleteTmpNotice")
	public String deleteTmpNotice(@RequestParam(value = "id", required = true) String id, RedirectAttributes attr, Model model) {
		try {
			contentService.deleteTmpNotice(id);
			attr.addFlashAttribute("msg", "删除临时公告成功");
			return "redirect:/content/tmpNotice";
		} catch (Exception e) {
			attr.addFlashAttribute("msg", "删除临时公告失败");
			Logger.error("删除临时公告失败：", e);
			return "redirect:/content/tmpNotice";
		}
	}

	/**
	 * 内容管理--预览
	 * 
	 * @author lishunfeng
	 */

	@RequestMapping("/preview")
	public String preview(@RequestParam("id") String id, Model model) {
		model.addAttribute("context", contentService.findOneById(id));
		return "/content/preview";
	}

	/**
	 * 图片管理结果页面
	 * 
	 * @author lishunfeng
	 */
	@RequestMapping("/imageIndex")
	public String imageData(@RequestParam(value = "page", defaultValue = "0") Integer page, @RequestParam(value = "size", defaultValue = "10") Integer size, Model model) {
		model.addAttribute("imageManages", contentService.findAllImageManage(page, size));
		return "/content/imageManageIndex";
	}

	/**
	 * 新增图片管理
	 * 
	 * @author lishunfeng
	 */
	@RequestMapping("/addImageManage")
	public String addImageManage(Model model) {
		return "/content/addImageManage";
	}

	/**
	 * 添加图片管理处理逻辑
	 * 
	 * @author lishunfeng
	 */
	@RequestMapping("/handerAddImageManage")
	public String handerAddImageManage(MultipartHttpServletRequest request, RedirectAttributes attr) {
		try {
			String type = request.getParameter("type");
			String name = request.getParameter("name");
			String link = request.getParameter("link");
			int order = Integer.parseInt(request.getParameter("order"));
			MultipartFile file = request.getFile("file");
			contentService.addImageManage(type, name, link, order, file);
			attr.addFlashAttribute("msg", "添加图片成功");
			return "redirect:/content/imageIndex";
		} catch (Exception e) {
			attr.addFlashAttribute("msg", "添加图片失败");
			Logger.error("添加链接失败：", e);
			return "redirect:/content/addImageManage";
		}
	}

	/**
	 * @description:图片查看
	 */
	@RequestMapping(value = "/picture/{id}", method = RequestMethod.GET)
	public void picture(HttpServletResponse response, @PathVariable String id) {
		ImageManage imageManage = contentService.findOneImageManage(id);
		try {
			response.setContentType("image/jpeg");
			if (imageManage.getImage() != null) {
				response.getOutputStream().write(imageManage.getImage());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 图片管理删除
	 * 
	 * @author lishunfeng
	 */
	@RequestMapping("/deleteImageManage")
	public String deleteImageManage(@RequestParam(value = "id", required = true) String id, RedirectAttributes attr, Model model) {
		try {
			contentService.deleteImageManage(id);
			attr.addFlashAttribute("msg", "删除图片管理成功");
			return "redirect:/content/imageIndex";
		} catch (Exception e) {
			attr.addFlashAttribute("msg", "删除图片管理失败");
			Logger.error("删除图片管理失败：", e);
			return "redirect:/content/imageIndex";
		}
	}

	/**
	 * 图片管理编辑
	 * 
	 * @author lishunfeng
	 */
	@RequestMapping("/editImageManage")
	public String editImageManage(@RequestParam(value = "id", required = true) String id, Model model) {
		model.addAttribute("imageManage", contentService.findOneImageManage(id));
		return "/content/editImageManage";
	}

	/**
	 * 点击编辑图片管理页面保存按钮处理逻辑
	 * 
	 * @author lishunfeng
	 */
	@RequestMapping("/handerEditImageManage")
	public String handerEditImageManage(MultipartHttpServletRequest request, RedirectAttributes attr, Model model) {
		try {
			String id = request.getParameter("id");
			String type = request.getParameter("type");
			String name = request.getParameter("name");
			String link = request.getParameter("link");
			int order = Integer.parseInt(request.getParameter("order"));
			MultipartFile file = request.getFile("file");
			contentService.updateImageManage(id, type, name, link, order, file);
			attr.addFlashAttribute("msg", "添加图片成功");
			return "redirect:/content/imageIndex";
		} catch (Exception e) {
			attr.addFlashAttribute("msg", "添加图片失败");
			Logger.error("添加链接失败：", e);
			return "redirect:/content/addImageManage";
		}
	}

}
