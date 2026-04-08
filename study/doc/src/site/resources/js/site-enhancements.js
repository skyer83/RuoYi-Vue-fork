(function() {
  document.addEventListener('DOMContentLoaded', function() {
    // ========== 搜索功能（使用 MiniSearch）==========
    var miniSearch;
    var searchIndexLoaded = false;
    
    function initSearch() {
      var bodyColumn = document.getElementById('bodyColumn');
      if (!bodyColumn) return;
      
      // 创建搜索容器 - 放在 bodyColumn 右上角
      var searchContainer = document.createElement('div');
      searchContainer.className = 'search-container';
      searchContainer.style.cssText = 'position:absolute;top:-12px;right:-18px;width:250px;z-index:10000;';
      
      var searchInput = document.createElement('input');
      searchInput.type = 'text';
      searchInput.className = 'search-input';
      searchInput.placeholder = '搜索文档...';
      searchInput.style.cssText = 'width:100%;padding:6px 10px;border:1px solid #ccc;border-radius:4px;font-size:13px;box-sizing:border-box;';
      
      var searchResults = document.createElement('div');
      searchResults.className = 'search-results';
      searchResults.style.cssText = 'position:absolute;top:100%;left:0;right:0;margin-top:5px;max-height:400px;overflow-y:auto;background:white;border:1px solid #ddd;border-radius:4px;box-shadow:0 4px 12px rgba(0,0,0,0.15);z-index:10001;display:none;';
      
      searchContainer.appendChild(searchInput);
      searchContainer.appendChild(searchResults);
      bodyColumn.style.position = 'relative';
      bodyColumn.insertBefore(searchContainer, bodyColumn.firstChild);
      
      // 初始化 MiniSearch
      miniSearch = new MiniSearch({
        fields: ['title', 'content'],
        storeFields: ['title', 'url', 'content'],
        tokenize: function(text) {
          // 自定义分词：保留技术术语如 ISO-8859-1，同时支持中文
          // 将连字符、下划线、点号连接的单词视为一个token
          return text.toLowerCase().split(/[^\w\u4e00-\u9fa5\-_.]+/).filter(function(t) { return t.length > 0; });
        },
        searchOptions: {
          boost: { title: 3, content: 1 },
          fuzzy: function(term) { return term.length > 4 ? 0.2 : 0; },
          prefix: true
        }
      });
      
      // 加载索引
      loadSearchIndex();
      
      // 搜索输入事件
      searchInput.addEventListener('input', function() {
        var query = this.value.trim();
        if (query.length < 2) {
          searchResults.style.display = 'none';
          return;
        }
        performSearch(query, searchResults);
      });
      
      // ESC 键关闭搜索
      searchInput.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
          searchResults.style.display = 'none';
          this.value = '';
        }
      });
      
      // 点击外部关闭搜索
      document.addEventListener('click', function(e) {
        if (!searchContainer.contains(e.target)) {
          searchResults.style.display = 'none';
        }
      });
    }
    
    function loadSearchIndex() {
      var links = document.querySelectorAll('#navcolumn a[href$=".html"]');
      var documents = [];
      var id = 0;
      var pendingFetches = 0;
      
      links.forEach(function(link) {
        var href = link.getAttribute('href').split('#')[0];
        var title = link.textContent.trim();
        documents.push({
          id: id++,
          url: href,
          title: title,
          content: title // 初始内容为标题
        });
      });
      
      // 先添加文档（标题）
      miniSearch.addAll(documents);
      searchIndexLoaded = true;
      
      // 异步获取内容并更新索引
      documents.forEach(function(doc) {
        pendingFetches++;
        fetch(doc.url)
          .then(function(r) { 
            if (!r.ok) throw new Error('Fetch failed');
            return r.text(); 
          })
          .then(function(html) {
            var tempDiv = document.createElement('div');
            tempDiv.innerHTML = html;
            var scripts = tempDiv.querySelectorAll('script, style, nav, #banner, #breadcrumbs, #footer');
            scripts.forEach(function(s) { s.remove(); });
            var content = (tempDiv.textContent || tempDiv.innerText || '').trim().substring(0, 5000);
            
            // 清理内容：移除多余空白，但保留关键字符
            content = content.replace(/\s+/g, ' ');
            
            // 使用 replace 方法更新文档
            if (content && content.length > doc.title.length) {
              miniSearch.replace({
                id: doc.id,
                url: doc.url,
                title: doc.title,
                content: content
              });
              console.log('Indexed:', doc.title, 'Content length:', content.length);
            }
          })
          .catch(function(err) {
            console.log('Failed to fetch content for:', doc.url, err);
          })
          .finally(function() {
            pendingFetches--;
          });
      });
    }
    
    function performSearch(query, resultsContainer) {
      if (!searchIndexLoaded) {
        resultsContainer.innerHTML = '<div style="padding:20px;text-align:center;color:#667eea;">正在加载索引...</div>';
        resultsContainer.style.display = 'block';
        setTimeout(function() { performSearch(query, resultsContainer); }, 500);
        return;
      }
      
      // console.log('Searching for:', query);
      // console.log('Total indexed documents:', miniSearch.documentCount);
      
      // 使用更精确的搜索选项
      var results = miniSearch.search(query, {
        boost: { title: 3, content: 1 },
        fuzzy: 0.3,
        prefix: true
      });
      
      // console.log('Search results:', results.length);
      
      if (results.length === 0) {
        resultsContainer.innerHTML = '<div style="padding:20px;text-align:center;color:#999;">未找到匹配结果</div>';
      } else {
        resultsContainer.innerHTML = results.slice(0, 10).map(function(r) {
          var highlightedTitle = highlightText(r.title, query);
          // 生成内容预览
          var preview = '';
          if (r.content) {
            var lowerQuery = query.toLowerCase();
            var lowerContent = r.content.toLowerCase();
            var index = lowerContent.indexOf(lowerQuery);
            if (index >= 0) {
              var start = Math.max(0, index - 40);
              var end = Math.min(r.content.length, index + query.length + 60);
              preview = (start > 0 ? '...' : '') + r.content.substring(start, end) + (end < r.content.length ? '...' : '');
              preview = highlightText(preview, query);
            } else {
              preview = r.content.substring(0, 100) + '...';
            }
          }
          return '<div class="search-result-item" data-url="' + r.url + '" style="padding:10px 12px;border-bottom:1px solid #eee;cursor:pointer;transition:background 0.2s;" onmouseover="this.style.background=\'#f0f0f0\'" onmouseout="this.style.background=\'\'">' +
                 '<div style="font-weight:bold;color:#333;margin-bottom:4px;">' + highlightedTitle + '</div>' +
                 (preview ? '<div style="font-size:12px;color:#666;line-height:1.4;">' + preview + '</div>' : '') +
                 '</div>';
        }).join('');
        
        resultsContainer.querySelectorAll('.search-result-item').forEach(function(item) {
          item.addEventListener('click', function() {
            window.location.href = this.getAttribute('data-url');
          });
        });
      }
      resultsContainer.style.display = 'block';
    }
    
    function highlightText(text, query) {
      if (!query) return text;
      var regex = new RegExp('(' + escapeRegex(query) + ')', 'gi');
      return text.replace(regex, '<span class="search-highlight">$1</span>');
    }
    
    function escapeRegex(str) {
      return str.replace(/[\\/*+?^()[\]{}|]/g, function(m) { return '\\' + m; });
    }

    // ========== 侧边栏拖拽调整宽度 ==========
    var leftColumn = document.getElementById('leftColumn');
    var bodyColumn = document.getElementById('bodyColumn');
    var navColumn = document.getElementById('navcolumn');
    if (!leftColumn || !bodyColumn) return;

    bodyColumn.style.marginLeft = (parseInt(leftColumn.offsetWidth) + 10 + 20) + 'px';

    var resizer = document.createElement('div');
    resizer.id = 'menuResizer';
    resizer.style.cssText = 'position:absolute;right:0;top:0;bottom:0;width:5px;cursor:col-resize;background:transparent;z-index:1003';
    leftColumn.style.position = 'relative';
    leftColumn.appendChild(resizer);

    var isDragging = false;
    var startX = 0;
    var startWidth = 0;

    resizer.addEventListener('mousedown', function(event) {
      isDragging = true;
      startX = event.clientX;
      startWidth = leftColumn.offsetWidth;
      document.body.style.cursor = 'col-resize';
      document.body.style.userSelect = 'none';
      event.preventDefault();
    });

    document.addEventListener('mousemove', function(event) {
      if (!isDragging) return;
      var newWidth = Math.max(180, Math.min(350, startWidth + (event.clientX - startX)));
      leftColumn.style.setProperty('width', newWidth + 'px', 'important');
      leftColumn.style.maxWidth = newWidth + 'px';
      bodyColumn.style.marginLeft = (newWidth + 20) + 'px';
      localStorage.setItem('menuWidth', newWidth);
    });

    document.addEventListener('mouseup', function() {
      if (isDragging) {
        isDragging = false;
        document.body.style.cursor = '';
        document.body.style.userSelect = '';
      }
    });

    resizer.addEventListener('mouseenter', function() { this.style.background = '#ccc'; });
    resizer.addEventListener('mouseleave', function() { if (!isDragging) this.style.background = 'transparent'; });

    var savedWidth = localStorage.getItem('menuWidth');
    if (savedWidth) {
      savedWidth = parseInt(savedWidth);
      leftColumn.style.setProperty('width', savedWidth + 'px', 'important');
      leftColumn.style.maxWidth = savedWidth + 'px';
      bodyColumn.style.marginLeft = (savedWidth + 20) + 'px';
    }

    // ========== 收起/展开整个侧边栏按钮 ==========
    var sidebarToggleBtn = document.createElement('button');
    sidebarToggleBtn.id = 'sidebarToggleBtn';
    sidebarToggleBtn.innerHTML = '<<';
    sidebarToggleBtn.style.cssText = 'position:absolute;right:0;top:0;width:24px;padding:0;background:#f5f5f5;color:#666;border:none;border-left:1px solid #ddd;cursor:pointer;z-index:1002;font-size:14px;font-weight:bold;transition:background 0.2s;';
    sidebarToggleBtn.onmouseenter = function() { this.style.background = '#e0e0e0'; };
    sidebarToggleBtn.onmouseleave = function() { this.style.background = '#f5f5f5'; };
    leftColumn.appendChild(sidebarToggleBtn);

    var isSidebarCollapsed = localStorage.getItem('sidebarCollapsed') === 'true';
    var originalWidth = parseInt(localStorage.getItem('sidebarOriginalWidth')) || 350;
    var collapsedWidth = 24;

    function updateToggleBtn() {
      if (isSidebarCollapsed) {
        sidebarToggleBtn.innerHTML = '>>';
        sidebarToggleBtn.style.right = '0';
      } else {
        sidebarToggleBtn.innerHTML = '<<';
        sidebarToggleBtn.style.right = '5px';
      }
    }

    var resizerElement = document.getElementById('menuResizer');

    function toggleSidebar() {
      if (isSidebarCollapsed) {
        leftColumn.style.setProperty('width', originalWidth + 'px', 'important');
        leftColumn.style.maxWidth = originalWidth + 'px';
        leftColumn.style.minWidth = '180px';
        leftColumn.style.overflowX = 'auto';
        bodyColumn.style.marginLeft = (originalWidth + 20) + 'px';
        if (navColumn) navColumn.style.visibility = 'visible';
        if (resizerElement) resizerElement.style.visibility = 'visible';
        localStorage.setItem('sidebarCollapsed', 'false');
      } else {
        originalWidth = leftColumn.offsetWidth;
        localStorage.setItem('sidebarOriginalWidth', originalWidth);
        leftColumn.style.setProperty('width', collapsedWidth + 'px', 'important');
        leftColumn.style.maxWidth = collapsedWidth + 'px';
        leftColumn.style.minWidth = collapsedWidth + 'px';
        leftColumn.style.overflowX = 'hidden';
        bodyColumn.style.marginLeft = (collapsedWidth + 20) + 'px';
        if (navColumn) navColumn.style.visibility = 'hidden';
        if (resizerElement) resizerElement.style.visibility = 'hidden';
        localStorage.setItem('sidebarCollapsed', 'true');
      }
      isSidebarCollapsed = !isSidebarCollapsed;
      updateToggleBtn();
    }

    sidebarToggleBtn.addEventListener('click', function(event) {
      event.preventDefault();
      event.stopPropagation();
      toggleSidebar();
    });

    if (isSidebarCollapsed) {
      leftColumn.style.setProperty('width', collapsedWidth + 'px', 'important');
      leftColumn.style.maxWidth = collapsedWidth + 'px';
      leftColumn.style.minWidth = collapsedWidth + 'px';
      leftColumn.style.overflow = 'hidden';
      bodyColumn.style.marginLeft = (collapsedWidth + 20) + 'px';
      if (navColumn) navColumn.style.visibility = 'hidden';
      if (resizerElement) resizerElement.style.visibility = 'hidden';
    }
    updateToggleBtn();

    // ========== 一键展开/收缩按钮 ==========
    navColumn = document.getElementById('navcolumn');
    if (navColumn && !document.getElementById('expandCollapseButtons')) {
      var buttonContainer = document.createElement('div');
      buttonContainer.id = 'expandCollapseButtons';
      buttonContainer.style.cssText = 'padding:8px 10px;border-bottom:1px solid #ddd;margin-bottom:10px;display:flex;gap:8px;';

      var expandAllBtn = document.createElement('button');
      expandAllBtn.textContent = '展开全部';
      expandAllBtn.style.cssText = 'flex:1;padding:4px 8px;font-size:12px;cursor:pointer;background:#667eea;color:#fff;border:none;border-radius:4px;';

      var collapseAllBtn = document.createElement('button');
      collapseAllBtn.textContent = '收起全部';
      collapseAllBtn.style.cssText = 'flex:1;padding:4px 8px;font-size:12px;cursor:pointer;background:#764ba2;color:#fff;border:none;border-radius:4px;';

      buttonContainer.appendChild(expandAllBtn);
      buttonContainer.appendChild(collapseAllBtn);
      navColumn.insertBefore(buttonContainer, navColumn.firstChild);

      expandAllBtn.addEventListener('click', function() {
        document.querySelectorAll('#navcolumn li').forEach(function(menuItem) {
          if (menuItem.querySelector(':scope > ul')) {
            menuItem.classList.remove('collapsed');
            menuItem.classList.add('expanded');
          }
        });
        localStorage.removeItem('menuExpanded');
      });

      collapseAllBtn.addEventListener('click', function() {
        var menuStates = {};
        document.querySelectorAll('#navcolumn li').forEach(function(menuItem) {
          if (menuItem.querySelector(':scope > ul')) {
            menuItem.classList.remove('expanded');
            menuItem.classList.add('collapsed');
            var menuLink = menuItem.querySelector(':scope > a') || menuItem.querySelector(':scope > strong');
            if (menuLink) {
              var menuKey = menuLink.getAttribute('href') || menuLink.textContent;
              menuStates[menuKey] = false;
            }
          }
        });
        localStorage.setItem('menuExpanded', JSON.stringify(menuStates));
      });
    }

    // ========== 菜单折叠功能 ==========
    var savedMenuStates = JSON.parse(localStorage.getItem('menuExpanded') || '{}');
    document.querySelectorAll('#navcolumn li').forEach(function(menuItem) {
      var subMenu = menuItem.querySelector(':scope > ul');
      if (!subMenu) return;

      var menuLink = menuItem.querySelector(':scope > a') || menuItem.querySelector(':scope > strong');
      if (!menuLink) return;

      var linkHref = menuLink.getAttribute('href') || '';
      var menuKey = linkHref || menuLink.textContent;

      if (!menuItem.classList.contains('expanded') && !menuItem.classList.contains('collapsed')) {
        menuItem.classList.add('expanded');
      }
      if (savedMenuStates[menuKey] === false) {
        menuItem.classList.remove('expanded');
        menuItem.classList.add('collapsed');
      }

      menuItem.addEventListener('click', function(event) {
        if (event.target !== menuItem) return;
        event.preventDefault();
        event.stopPropagation();
        menuItem.classList.toggle('expanded');
        menuItem.classList.toggle('collapsed');
        savedMenuStates = JSON.parse(localStorage.getItem('menuExpanded') || '{}');
        savedMenuStates[menuKey] = menuItem.classList.contains('expanded');
        localStorage.setItem('menuExpanded', JSON.stringify(savedMenuStates));
      });
    });
    
    // 初始化搜索功能
    initSearch();
  });
})();
