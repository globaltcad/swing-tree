
// -----------------------------------------
// This is for html based dynamic content:

function switchTab(src, target) {
    const tabsWrapper = $(src.target).parent().parent();
    // Now we need to find the '.TabBody' element:
    // (but only the first child! because we don't want to traverse the whole tree
    // in order to avoid messing up nested tabs)
    const tabBody = tabsWrapper.children('.TabBody').first();
    tabBody.children().css("display", "none");
    $(src.target).siblings().removeClass("selected");
    $(src.target).parent().parent().find(target).css("display", "");
    $(src.target).addClass("selected");
    console.log($(src.target).html());
}


function applyMarkdown() {
    for (let item of document.getElementsByClassName("MarkdownMe")) {
        item.innerHTML = marked.parse(item.innerHTML)
        item.classList.remove("MarkdownMe");
        console.log("Converting to markdown in tag "+item.tagName);
    }
}


$(document).ready(()=>{
    applyMarkdown();
});