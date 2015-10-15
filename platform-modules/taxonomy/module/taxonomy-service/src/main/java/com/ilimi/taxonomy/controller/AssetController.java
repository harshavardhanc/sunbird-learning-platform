package com.ilimi.taxonomy.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.ilimi.common.controller.BaseController;
import com.ilimi.common.dto.Response;

@Controller
@RequestMapping("/v1/asset")
public class AssetController extends BaseController {

    @Autowired
    private ContentController contentController;

    private String taxonomyId = "assets";
    private String assetFolder = "assets";
    private String objectType = "Asset";

    @RequestMapping(value = "", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Response> create(@RequestBody Map<String, Object> map,
            @RequestHeader(value = "user-id") String userId) {
        return contentController.create(taxonomyId, objectType, map, userId);
    }

    @RequestMapping(value = "/{id:.+}", method = RequestMethod.PATCH)
    @ResponseBody
    public ResponseEntity<Response> update(@PathVariable(value = "id") String id, @RequestBody Map<String, Object> map,
            @RequestHeader(value = "user-id") String userId) {
        return contentController.update(id, taxonomyId, objectType, map, userId);
    }

    @RequestMapping(value = "/{id:.+}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Response> find(@PathVariable(value = "id") String id,
            @RequestParam(value = "fields", required = false) String[] fields,
            @RequestHeader(value = "user-id") String userId) {
        return contentController.find(id, taxonomyId, fields, userId);
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Response> findAll(@RequestParam(value = "offset", required = false) Integer offset,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "fields", required = false) String[] fields,
            @RequestHeader(value = "user-id") String userId) {
        return contentController.findAll(taxonomyId, objectType, offset, limit, fields, userId);
    }

    @RequestMapping(value = "/{id:.+}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<Response> delete(@PathVariable(value = "id") String id,
            @RequestHeader(value = "user-id") String userId) {
        return contentController.delete(id, taxonomyId, objectType, userId);
    }

    @RequestMapping(value = "/search", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Response> search(@RequestBody Map<String, Object> map,
            @RequestHeader(value = "user-id") String userId) {
        return contentController.search(taxonomyId, objectType, map, userId);
    }

    @RequestMapping(value = "/list", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Response> list(@RequestBody Map<String, Object> map) {
        return contentController.list(taxonomyId, objectType, map);
    }

    @RequestMapping(value = "/upload/{id:.+}", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Response> upload(@PathVariable(value = "id") String id,
            @RequestParam(value = "file", required = true) MultipartFile file,
            @RequestHeader(value = "user-id") String userId) {
        return contentController.upload(id, file, taxonomyId, userId, assetFolder);
    }

}
