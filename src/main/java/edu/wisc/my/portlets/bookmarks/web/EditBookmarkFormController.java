/**
 * Licensed to Apereo under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Apereo licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a
 * copy of the License at the following location:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package edu.wisc.my.portlets.bookmarks.web;

import java.util.Date;
import java.util.Map;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletRequest;

import org.apache.commons.lang.StringUtils;
import org.springframework.validation.BindException;

import edu.wisc.my.portlets.bookmarks.domain.Bookmark;
import edu.wisc.my.portlets.bookmarks.domain.BookmarkSet;
import edu.wisc.my.portlets.bookmarks.domain.Entry;
import edu.wisc.my.portlets.bookmarks.domain.Folder;
import edu.wisc.my.portlets.bookmarks.domain.support.FolderUtils;
import edu.wisc.my.portlets.bookmarks.domain.support.IdPathInfo;



/**
 * <p>EditBookmarkFormController class.</p>
 *
 * @author Eric Dalquist <a href="mailto:eric.dalquist@doit.wisc.edu">eric.dalquist@doit.wisc.edu</a>
 * @version $Revision: 12175 $
 */
public class EditBookmarkFormController extends BaseEntryFormController {
    /** {@inheritDoc} */
    @Override
    protected Object formBackingObject(PortletRequest request) throws Exception {
        //TODO if move return default object
        //TODO if no move get real object from store for updating
        return super.formBackingObject(request);
    }

    /** {@inheritDoc} */
    @Override
    protected void onSubmitAction(ActionRequest request, ActionResponse response, Object command, BindException errors) throws Exception {
        final String targetParentPath = StringUtils.defaultIfEmpty(request.getParameter("folderPath"), null);
        final String targetEntryPath = StringUtils.defaultIfEmpty(request.getParameter("idPath"), null);
        
        //User edited bookmark
        final Bookmark commandBookmark = (Bookmark)command;
        
        //Get the BookmarkSet from the store
        final BookmarkSet bs = this.bookmarkSetRequestResolver.getBookmarkSet(request, false);
        if (bs == null) {
            throw new IllegalArgumentException("No BookmarkSet exists for request='" + request + "'");
        }
        
        //Get the target parent folder
        final IdPathInfo targetParentPathInfo = FolderUtils.getEntryInfo(bs, targetParentPath);
        if (targetParentPathInfo == null || targetParentPathInfo.getTarget() == null) {
            throw new IllegalArgumentException("The specified parent Folder does not exist. BaseFolder='" + bs + "' and idPath='" + targetParentPath + "'");
        }
        
        final Folder targetParent = (Folder)targetParentPathInfo.getTarget();
        final Map<Long, Entry> targetChildren = targetParent.getChildren();
        
        //Get the original bookmark & it's parent folder
        final IdPathInfo originalBookmarkPathInfo = FolderUtils.getEntryInfo(bs, targetEntryPath);
        if (targetParentPathInfo == null || originalBookmarkPathInfo.getTarget() == null) {
            throw new IllegalArgumentException("The specified Bookmark does not exist. BaseFolder='" + bs + "' and idPath='" + targetEntryPath + "'");
        }
        
        final Folder originalParent = originalBookmarkPathInfo.getParent();
        final Bookmark originalBookmark = (Bookmark)originalBookmarkPathInfo.getTarget();

        //If moving the bookmark
        if (targetParent.getId() != originalParent.getId()) {
            final Map<Long, Entry> originalChildren = originalParent.getChildren();
            originalChildren.remove(originalBookmark.getId());
            
            commandBookmark.setCreated(originalBookmark.getCreated());
            commandBookmark.setModified(new Date());

            targetChildren.put(commandBookmark.getId(), commandBookmark);
        }
        //If just updaing the bookmark
        //TODO should the formBackingObject be smarter on form submits for editBookmark and return the targeted bookmark?
        else {
            originalBookmark.setModified(new Date());
            originalBookmark.setName(commandBookmark.getName());
            originalBookmark.setNote(commandBookmark.getNote());
            originalBookmark.setUrl(commandBookmark.getUrl());
            originalBookmark.setNewWindow(commandBookmark.isNewWindow());
        }

        //Persist the changes to the BookmarkSet 
        this.bookmarkStore.storeBookmarkSet(bs);
    }
}
