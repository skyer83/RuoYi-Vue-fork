/**
 * 左侧菜单栏拖拽调整宽度功能
 */
(function() {
    document.addEventListener('DOMContentLoaded', function() {
        var leftColumn = document.getElementById('leftColumn');
        var bodyColumn = document.getElementById('bodyColumn');
        
        if (!leftColumn || !bodyColumn) return;
        
        // 创建拖拽条
        var resizer = document.createElement('div');
        resizer.id = 'menuResizer';
        resizer.style.cssText = 'position: absolute; right: 0; top: 0; bottom: 0; width: 5px; cursor: col-resize; background: transparent; z-index: 1000;';
        leftColumn.style.position = 'relative';
        leftColumn.appendChild(resizer);
        
        // 拖拽状态
        var isResizing = false;
        var startX = 0;
        var startWidth = 0;
        
        // 鼠标按下
        resizer.addEventListener('mousedown', function(e) {
            isResizing = true;
            startX = e.clientX;
            startWidth = leftColumn.offsetWidth;
            
            // 添加拖拽样式
            document.body.style.cursor = 'col-resize';
            document.body.style.userSelect = 'none';
            
            e.preventDefault();
        });
        
        // 鼠标移动
        document.addEventListener('mousemove', function(e) {
            if (!isResizing) return;
            
            var width = startWidth + (e.clientX - startX);
            
            // 限制最小和最大宽度
            width = Math.max(180, Math.min(500, width));
            
            leftColumn.style.width = width + 'px';
            leftColumn.style.maxWidth = width + 'px';
            bodyColumn.style.marginLeft = (width + 20) + 'px';
            
            // 保存宽度到 localStorage
            localStorage.setItem('menuWidth', width);
        });
        
        // 鼠标释放
        document.addEventListener('mouseup', function() {
            if (isResizing) {
                isResizing = false;
                document.body.style.cursor = '';
                document.body.style.userSelect = '';
            }
        });
        
        // 鼠标悬停时显示拖拽条
        resizer.addEventListener('mouseenter', function() {
            this.style.background = '#ccc';
        });
        
        resizer.addEventListener('mouseleave', function() {
            if (!isResizing) {
                this.style.background = 'transparent';
            }
        });
        
        // 恢复保存的宽度
        var savedWidth = localStorage.getItem('menuWidth');
        if (savedWidth) {
            var width = parseInt(savedWidth);
            leftColumn.style.width = width + 'px';
            leftColumn.style.maxWidth = width + 'px';
            bodyColumn.style.marginLeft = (width + 20) + 'px';
        }
    });
})();
