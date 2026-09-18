package com.wolf.browser

object AdBlocker {
    val JS = """
    (function(){
        try{
            var sels=['ins.adsbygoogle','[data-ad-slot]','[data-ad-client]',
                '[id^="aswift_"]','[id^="google_ads"]','[id*="google_ads"]',
                'iframe[src*="doubleclick"]','iframe[src*="googlesyndication"]',
                'iframe[src*="googleadservices"]','iframe[src*="adservice"]',
                '[class*="advertisement"]','[class*="advert-"]','[class*="adsbygoogle"]',
                '[class*="ad-banner"]','[class*="ad-container"]','[class*="ad-wrapper"]',
                '[class*="popup-ad"]','[class*="sponsored"]','[aria-label*="advert"]',
                'div[id*="banner-ad"]','a[href*="doubleclick.net"]',
                'a[href*="googlesyndication"]','img[src*="doubleclick"]',
                'img[src*="adservice"]','.ads','.adsbox','#ad','#ads'];
            var c=0;
            for(var i=0;i<sels.length;i++){
                try{
                    var l=document.querySelectorAll(sels[i]);
                    for(var j=0;j<l.length;j++){
                        var e=l[j];
                        if(e&&e.style&&e.style.display!=='none'){
                            e.style.display='none';
                            e.style.visibility='hidden';
                            e.style.height='0px';
                            c++;
                        }
                    }
                }catch(e){}
            }
            window.open=function(){return null};
            window.__wolfAdCount=(window.__wolfAdCount||0)+c;
        }catch(e){}
    })();
    """.trimIndent()
}
