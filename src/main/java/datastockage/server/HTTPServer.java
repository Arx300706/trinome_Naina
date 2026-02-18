package server;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import dir.DirServer;
import common.FileMeta;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.*;

/**
 * Serveur HTTP REST pour le système de stockage distribué.
 * Accessible via http://10.134.17.222:8080
 * API REST + Interface web JS
 */
public class HTTPServer {

    private static final int PORT = 8080;
    private static final String BIND_ADDRESS = "0.0.0.0";
    
    private HttpServer server;
    private DirServer dirServer;

    public HTTPServer(DirServer dirServer) {
        this.dirServer = dirServer;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(BIND_ADDRESS, PORT), 50);
        
        server.createContext("/", new RootHandler());
        server.createContext("/ui", new UIHandler());
        server.createContext("/api/upload", new UploadHandler(dirServer));
        server.createContext("/api/download", new DownloadHandler(dirServer));
        server.createContext("/api/files", new ListFilesHandler(dirServer));
        server.createContext("/api/cluster", new ClusterStatusHandler(dirServer));
        
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(10));
        server.start();
        
        System.out.println("[HTTPServer] Web + API REST demarree sur http://0.0.0.0:" + PORT);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("[HTTPServer] API REST arretee");
        }
    }

    static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = "<html><head><meta charset=UTF-8><title>Cloud Distribue</title><style>" +
                "body{font-family:Arial;background:linear-gradient(135deg,#667eea,#764ba2);color:white;padding:40px;text-align:center}" +
                ".container{max-width:800px;margin:0 auto;background:rgba(255,255,255,0.1);padding:40px;border-radius:10px}" +
                "h1{margin-bottom:20px}a{color:white;text-decoration:none;display:inline-block;margin:10px;padding:10px 20px;background:#764ba2;border-radius:5px}" +
                "a:hover{background:#667eea}" +
                "</style></head><body><div class=container>" +
                "<h1>Cloud Distribue</h1>" +
                "<p>Stockage resilient accessible en reseau</p>" +
                "<a href=/ui>Interface Web</a>" +
                "<p style='margin-top:30px'>Serveur: http://10.134.17.222:8080</p>" +
                "</div></body></html>";
            
            byte[] response = html.getBytes("UTF-8");
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.getResponseBody().close();
        }
    }

    static class UIHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = getUIPage();
            byte[] response = html.getBytes("UTF-8");
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.getResponseBody().close();
        }
    }

    static class UploadHandler implements HttpHandler {
        private DirServer dirServer;

        UploadHandler(DirServer dirServer) {
            this.dirServer = dirServer;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equals("POST")) {
                sendError(exchange, 405, "Methode non autorisee");
                return;
            }

            try {
                InputStream is = exchange.getRequestBody();
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] data = new byte[8192];
                int nRead;
                while ((nRead = is.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                byte[] fileData = buffer.toByteArray();

                String query = exchange.getRequestURI().getQuery();
                Map<String, String> params = parseQuery(query);
                
                String fileName = params.getOrDefault("fileName", "unknown");
                String userId = params.getOrDefault("userId", "anonymous");

                if (dirServer.handleUploadData(userId, fileName, fileData)) {
                    sendJson(exchange, 200, "{\"status\":\"success\",\"message\":\"Fichier distribue\",\"fileName\":\"" + escape(fileName) + "\"}");
                } else {
                    sendError(exchange, 500, "Erreur upload");
                }
            } catch (Exception e) {
                sendError(exchange, 500, "Erreur: " + e.getMessage());
            }
        }
    }

    static class DownloadHandler implements HttpHandler {
        private DirServer dirServer;

        DownloadHandler(DirServer dirServer) {
            this.dirServer = dirServer;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equals("GET")) {
                sendError(exchange, 405, "Methode non autorisee");
                return;
            }

            try {
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> params = parseQuery(query);
                
                String fileName = params.getOrDefault("fileName", "");
                String userId = params.getOrDefault("userId", "anonymous");

                byte[] fileData = dirServer.handleDownloadRequest(fileName, userId);
                
                if (fileData != null) {
                    exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
                    exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=" + fileName);
                    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                    
                    exchange.sendResponseHeaders(200, fileData.length);
                    exchange.getResponseBody().write(fileData);
                    exchange.getResponseBody().close();
                } else {
                    sendError(exchange, 404, "Fichier non trouve");
                }
            } catch (Exception e) {
                sendError(exchange, 500, "Erreur: " + e.getMessage());
            }
        }
    }

    static class ListFilesHandler implements HttpHandler {
        private DirServer dirServer;

        ListFilesHandler(DirServer dirServer) {
            this.dirServer = dirServer;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

            try {
                Map<String, FileMeta> files = dirServer.getFiles();
                StringBuilder json = new StringBuilder("[");
                
                boolean first = true;
                for (FileMeta file : files.values()) {
                    if (!first) json.append(",");
                    json.append("{\"fileName\":\"").append(escape(file.fileName)).append("\",");
                    json.append("\"ownerId\":\"").append(escape(file.ownerId)).append("\",");
                    json.append("\"totalSize\":").append(file.totalSize).append(",");
                    json.append("\"chunks\":").append(file.chunkIds.size()).append("}");
                    first = false;
                }
                json.append("]");

                byte[] response = json.toString().getBytes("UTF-8");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.getResponseBody().close();
            } catch (Exception e) {
                sendError(exchange, 500, "Erreur: " + e.getMessage());
            }
        }
    }

    static class ClusterStatusHandler implements HttpHandler {
        private DirServer dirServer;

        ClusterStatusHandler(DirServer dirServer) {
            this.dirServer = dirServer;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

            try {
                String status = dirServer.getClusterStatus();
                byte[] response = status.getBytes("UTF-8");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.getResponseBody().close();
            } catch (Exception e) {
                sendError(exchange, 500, "Erreur: " + e.getMessage());
            }
        }
    }

    private static void sendJson(HttpExchange exchange, int code, String json) throws IOException {
        byte[] response = json.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(code, response.length);
        exchange.getResponseBody().write(response);
        exchange.getResponseBody().close();
    }

    private static void sendError(HttpExchange exchange, int code, String message) throws IOException {
        String json = "{\"error\":\"" + escape(message) + "\"}";
        byte[] response = json.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(code, response.length);
        exchange.getResponseBody().write(response);
        exchange.getResponseBody().close();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) return params;
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2) {
                try {
                    params.put(kv[0], java.net.URLDecoder.decode(kv[1], "UTF-8"));
                } catch (Exception e) {
                    params.put(kv[0], kv[1]);
                }
            }
        }
        return params;
    }

    private static String getUIPage() {
        String api = "http://10.134.17.222:8080";
        return "<!DOCTYPE html><html><head><meta charset=UTF-8><meta name=viewport content='width=device-width'>" +
            "<title>Client Cloud</title><style>" +
            "body{font-family:Arial;background:linear-gradient(135deg,#667eea,#764ba2);padding:20px;min-height:100vh}" +
            ".container{max-width:900px;margin:0 auto}" +
            "header{color:white;text-align:center;margin-bottom:30px}" +
            ".main{display:grid;grid-template-columns:1fr 1fr;gap:20px}" +
            "@media(max-width:768px){.main{grid-template-columns:1fr}}" +
            ".card{background:white;padding:20px;border-radius:10px;box-shadow:0 5px 15px rgba(0,0,0,0.2)}" +
            ".card h2{color:#667eea;margin-bottom:15px}" +
            ".upload-area{border:3px dashed #667eea;padding:30px;text-align:center;border-radius:8px;cursor:pointer;background:#f8f9ff}" +
            ".upload-area:hover{background:#f0f2ff;" +
            ".upload-area.drag{background:#e8ebff}" +
            ".btn{background:linear-gradient(135deg,#667eea,#764ba2);color:white;border:none;padding:10px 20px;border-radius:5px;cursor:pointer;width:100%;margin-top:10px}" +
            ".btn:hover{opacity:0.9}" +
            ".btn:disabled{opacity:0.5}" +
            ".file-item{padding:10px;background:#f8f9ff;margin:10px 0;border-radius:5px;display:flex;justify-content:space-between;align-items:center}" +
            ".file-list{max-height:400px;overflow-y:auto}" +
            ".status{padding:10px;margin-top:10px;border-radius:5px;display:none}" +
            ".status.ok{background:#d4edda;color:#155724;display:block}" +
            ".status.err{background:#f8d7da;color:#721c24;display:block}" +
            "</style></head><body>" +
            "<div class=container>" +
            "<header><h1>Client Cloud</h1><p>" + api + "</p></header>" +
            "<div class=main>" +
            "<div class=card><h2>Upload</h2>" +
            "<div class=upload-area id=area>Glissez un fichier ici</div>" +
            "<div id=info style='display:none;margin-top:15px'>" +
            "<p><strong>Fichier:</strong> <span id=fname></span></p>" +
            "<p><strong>Taille:</strong> <span id=fsize></span></p>" +
            "</div>" +
            "<button class=btn id=upbtn style='display:none'>Uploader</button>" +
            "<div id=status class=status></div>" +
            "</div>" +
            "<div class=card><h2>Fichiers</h2>" +
            "<button class=btn id=refbtn>Actualiser</button>" +
            "<div class=file-list id=list><p style='text-align:center;color:#999'>Cliquez sur Actualiser</p></div>" +
            "</div></div></div>" +
            "<input type=file id=file style='display:none'>" +
            "<script>" +
            "const API='" + api + "';" +
            "const USER='u_'+Math.random().toString(36).slice(2,9);" +
            "let sf=null;" +
            "const area=document.getElementById('area');" +
            "const file=document.getElementById('file');" +
            "const upbtn=document.getElementById('upbtn');" +
            "const refbtn=document.getElementById('refbtn');" +
            "area.onclick=()=>file.click();" +
            "area.ondragover=(e)=>{e.preventDefault();area.classList.add('drag')};" +
            "area.ondragleave=()=>area.classList.remove('drag');" +
            "area.ondrop=(e)=>{e.preventDefault();area.classList.remove('drag');handle(e.dataTransfer.files[0])};" +
            "file.onchange=(e)=>handle(e.target.files[0]);" +
            "function handle(f){sf=f;document.getElementById('info').style.display='block';document.getElementById('fname').textContent=f.name;document.getElementById('fsize').textContent=fmt(f.size);upbtn.style.display='block'}" +
            "upbtn.onclick=async()=>{if(!sf)return;upbtn.disabled=true;try{const r=await fetch(API+'/api/upload?fileName='+encodeURIComponent(sf.name)+'&userId='+USER,{method:'POST',body:sf});const d=await r.json();msg('status','ok','Fichier distribue!');sf=null;document.getElementById('info').style.display='none';upbtn.style.display='none';file.value='';setTimeout(list,1000)}catch(e){msg('status','err','Erreur: '+e.message)}finally{upbtn.disabled=false}};" +
            "refbtn.onclick=list;" +
            "async function list(){try{const r=await fetch(API+'/api/files');const files=await r.json();const l=document.getElementById('list');if(files.length===0){l.innerHTML='<p style=text-align:center;color:#999>Aucun fichier</p>';return}l.innerHTML=files.map(f=>'<div class=file-item><div><div style=font-weight:bold>'+f.fileName+'</div><div style=color:#999;font-size:0.9em>'+fmt(f.totalSize)+' | '+f.chunks+' chunks</div></div><button class=btn style=padding:5px;margin:0 onclick=\"dl(\\''+f.fileName.replace(/'/g,\"\\\\'\")+'\\')\" >DL</button></div>').join('')}catch(e){document.getElementById('list').innerHTML='<p style=color:red>Erreur: '+e.message}}" +
            "async function dl(fn){try{const r=await fetch(API+'/api/download?fileName='+encodeURIComponent(fn)+'&userId='+USER);const b=await r.blob();const a=document.createElement('a');a.href=URL.createObjectURL(b);a.download=fn;document.body.appendChild(a);a.click();document.body.removeChild(a)}catch(e){alert('Erreur: '+e.message)}}" +
            "function fmt(b){if(b===0)return'0 B';const k=1024,s=['B','KB','MB','GB'],i=Math.floor(Math.log(b)/Math.log(k));return Math.round(b/Math.pow(k,i)*100)/100+' '+s[i]}" +
            "function msg(id,cls,txt){const e=document.getElementById(id);e.className='status '+cls;e.textContent=txt}" +
            "list();setInterval(list,30000);" +
            "</script></body></html>";
    }
}
