/*
 * Tarea 1 - Desarrollo de Aplicaciones Web.
 * Proyecto de Java para implementar un servidor HTTP simple con los metodos GET, HEAD y POST.
 * Mayrene Anchía Quesada - B40371
 * Patrick Loney Rojas - B43834.
 */

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Date;
import java.util.List;
import java.sql.Timestamp;
import java.util.ArrayList;

// Cada connecion de Cliente sera manejada en un hilo dedicado.
public class ServidorHTTP implements  Runnable {
    static final File RAIZ_WEB = new File(".");
    static final String ARCHIVO_DEFECTO = "index.html";
    static final String NO_ENCONTRADO = "404.html";
    static final String METODO_NO_ENCONTRADO = "no_soportado.html";

    String datosBitacora = ""; // String que contiene todos los datos de la bitacora.
    File archivoBitacora = new File("bitacoraServidorHTTP.txt"); // Archivo en el que se escribe la bitacora.

    // Puerto para levantar servidor.
    static final int PUERTO = 8080;

    // Para la conneccion del Cliente se utiliza la clase Socket.
    private Socket conectar;

    public ServidorHTTP(Socket c) {
        conectar = c;
        // Se asignan los encabezados de las columnas para la bitacora.
        datosBitacora += "_________________________________________________________________________________________________________________________________________" + "\n";
        datosBitacora += String.format("%10s %15s %20s %40s %30s %15s", "Metodo", "Estampilla", "Servidor", "Refiere", "Url", "Datos") + "\n";
        // datosBitacora += "\n";

    }

    public static void main(String[] args) {
        try {
            // Para levantar el servidor utilizamos la clase ServerSocket.
            ServerSocket levantarServidor = new ServerSocket(PUERTO);
            System.out.println("Servidor inicializado... \nEscuchando conexiones en el puerto: " + PUERTO + ". \n");

            // El servidor escucha hasta que el usuario pare su ejecución.
            while(true) {
                ServidorHTTP miServidor = new ServidorHTTP(levantarServidor.accept());
                System.out.println("Conexion iniciada. (" + new Date() + ")");

                // Crear hilo dedicado para manejar la conexion del cliente
                Thread hilo = new Thread(miServidor);
                hilo.start();

            }

        } catch(IOException e) {
            System.err.println("Error de conexion en el Servidor: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        // Aqui manejamos nuestra conexion de cliente particular.
        BufferedReader entrada = null; // Para leer caracteres del cliente.
        PrintWriter salida = null; // Se obtiene el flujo de caracteres de salida para el cliente (para headers/encabezados).
        BufferedOutputStream datosSalida = null; // Se obtiene el flujo binario de salida para el cliente (para datos solicitados).
        String archivoSolicitado = null; // Aqui se guarda el nombre del archivo solicitado por el cliente.

        try {
            // Se leen caracteres del cliente a traves del "input stream" o flujo de entrada en el socket.
            entrada = new BufferedReader(new InputStreamReader(conectar.getInputStream()));
            // Se obtiene el flujo de caracteres de salida para el cliente (headers).
            salida = new PrintWriter(conectar.getOutputStream());
            // Salida binaria para cliente (datos).
            datosSalida = new BufferedOutputStream(conectar.getOutputStream());

            // Para obtener la primera linea de solicitud del cliente
            String lineaE = entrada.readLine();
            // Analizamos la solicitud con StringTokenizer
            StringTokenizer analizar = new StringTokenizer(lineaE);
            String metodo = analizar.nextToken().toUpperCase(); // Se obtiene el metodo HTTP de la solicitud del cliente.
            archivoSolicitado = analizar.nextToken().toLowerCase(); // Se obtiene el archivo solicitado.

            // Se revisa si el metodo es un GET, un HEAD o un POST.
            if(!metodo.equals("GET") && !metodo.equals("HEAD") && !metodo.equals("POST")) {
                System.out.println("501, no implementado, metodo: " + metodo);

                // Y si no es ninguno de ellos, se devuelve el archivo de "no soportado" al cliente.
                File archivoNE = new File(RAIZ_WEB, METODO_NO_ENCONTRADO);
                int longiArchivo = (int) archivoNE.length();
                String tMIMEContenido = "text/html"; // Tipo de media del archivo.
                byte[] datosArch = leerDatosArchivo(archivoNE, longiArchivo); // Lee el contenido para retornarselo al cliente.

                // Se mandan Encabezados o Headers HTTP con datos al cliente.
                salida.println("HTTP/1.1 - 501 No Implementado");
                salida.println("Servidor (Server): Servidor HTTP - TP1");
                salida.println("Fecha (Date): " + new Date());
                salida.println("Tipo de contenido (Content-type): " + tMIMEContenido);
                salida.println("Longitud de contenido (Content-length): " + longiArchivo);
                salida.println("Acepta (Accept): text/html, text/plain"); // Tipos de media que acepta el servidor. Faltan imagenes.
                salida.println(); // Linea en blanco entre encabezados y contenido. Esto es importante!
                salida.flush(); // Se limpia el flujo de caracteres de salida.

                // Escribir datos a archivo.
                datosSalida.write(datosArch, 0, longiArchivo);
                datosSalida.flush();
            } else {
                // Es un metodo GET, HEAD o POST.

                // Si es GET o HEAD:
                if(metodo.equals("GET") || metodo.equals("HEAD")) {
                    if(archivoSolicitado.endsWith("/")) {
                        archivoSolicitado += ARCHIVO_DEFECTO;
                    }

                    boolean enviaDatos = false;
                    String datosGet = "";
                    if(archivoSolicitado.contains("?")){
                        StringTokenizer strtok = new StringTokenizer(archivoSolicitado, "?");
                        archivoSolicitado = strtok.nextToken() + ".html";
                        datosGet = strtok.nextToken();
                        enviaDatos = true;
                    }


                    File archivo = new File(RAIZ_WEB, archivoSolicitado);
                    int longiArchivo = (int) archivo.length();
                    String tMIMEContenido = getTipoContenido(archivoSolicitado);

                    String linea = "";
                    StringBuilder builder = new StringBuilder();
                    while ((linea = entrada.readLine()) != null && (linea.length() != 0)) {
                        builder.append(linea);
                        builder.append("\n");
                    }
                    String respuesta = builder.toString(); // Hilera con todos los headers recuperados con el string builder.
                    String[] lineasEncabezados = respuesta.split("\n"); // Partimos la hilera cada vez que hay un \n. Esto para tener cada header en un campo del arreglo.
                    HashMap mapaEncabezados = new HashMap<>(); // Para poder sacar solo los headers que queremos.
                    String[] encabezadoValor = new String[2]; // Vector auxiliar para guardar los headers como llave-valor en el mapa.
                    for(int i=0; i < lineasEncabezados.length; i++) { // Se recorre vector con los encabezados.
                        encabezadoValor = lineasEncabezados[i].split(" "); // Separa los encabezados en llave-valor y lo guarda en el arreglo encabezado-valor.
                        mapaEncabezados.put(encabezadoValor[0], encabezadoValor[1]); // Guarda las posciones del arreglo en el mapa.
                        encabezadoValor = new String[2]; // Limpiamos el vector auxiliar.
                    }

                    // Encabezados HTTP: Accept, Content-type, Content-length, Date, Host, Referer, Server.
                    salida.println("HTTP/1.1 - 200 OK");
                    salida.println("Accept: " + mapaEncabezados.get("Accept:"));
                    salida.println("Content-type: " + tMIMEContenido);
                    salida.println("Content-length: " + longiArchivo);
                    salida.println("Date: " + new Date());
                    salida.println("Host: " + mapaEncabezados.get("Host:"));
                    salida.println("Referer: " + mapaEncabezados.get("Referer:"));
                    salida.println("Server: Servidor HTTP - TP1");
                    salida.println(""); // Linea en blanco entre encabezados y contenido. Esto es importante!
                    salida.flush(); // Se limpia el flujo de caracteres de salida.

                    if(metodo.equals("GET")) { // Como es el metodo GET se devuelve el contenido.
                        if(enviaDatos == false) {
                            byte[] datosArchivo = leerDatosArchivo(archivo, longiArchivo);
                            // Escribir datos o contenido a archivo que se le muestra al cliente.
                            datosSalida.write(datosArchivo, 0, longiArchivo);
                            datosSalida.flush();
                        } else{
                            salida.println("<H1>Datos que se enviaron con metodo GET: " + datosGet + "! </H1>");
                            salida.flush();
                        }



                        // Escribe a bitacora.
                        escribirBitacora(metodo, ""+mapaEncabezados.get("Host:"), ""+mapaEncabezados.get("Referer:"), archivoSolicitado, datosGet);
                    } else {
                        // Escribe a bitacora.
                        escribirBitacora(metodo, ""+mapaEncabezados.get("Host:"), ""+mapaEncabezados.get("Referer:"), archivoSolicitado, "");
                    }
                    System.out.println("Archivo " + archivoSolicitado + " del tipo " + tMIMEContenido); // Imprime el archivo solicitado y su tipo.
                } else if(metodo.equals("POST")) {
                    // Implementar POST.
                    try {
                        String linea; // Para guardar la primera linea de solicitud del cliente.
                        /*
                        linea = entrada.readLine(); //Lee la primera linea.
                        String request_method = linea; //Obtiene el método solicitado.
                        System.out.println("HTTP-HEADER: " + linea); //Lo imprime.
                        */
                        linea = ""; // Limpia la variable.
                        // Busca post data
                        int postDataI = -1;
                        StringBuilder builder = new StringBuilder();
                        while((linea = entrada.readLine()) != null && (linea.length() != 0)) {
                            System.out.println("HTTP-HEADER: " + linea);
                            builder.append(linea);
                            builder.append("\n");
                            if(linea.indexOf("Content-Length:") > -1) {
                                postDataI = new Integer(
                                        linea.substring(
                                                linea.indexOf("Content-Length:") + 16,
                                                linea.length())).intValue();
                            }
                        }
                        String postData = "";
                        // Lee el post data
                        if(postDataI > 0) {
                            char[] charArray = new char[postDataI];
                            entrada.read(charArray, 0, postDataI);
                            postData = new String(charArray);
                        }

                        String respuesta = builder.toString(); // Hilera con todos los headers recuperados con el string builder.
                        String[] lineasEncabezados = respuesta.split("\n"); // Partimos la hilera cada vez que hay un \n. Esto para tener cada header en un campo del arreglo.
                        HashMap mapaEncabezados = new HashMap<>(); // Para poder sacar solo los headers que queremos.
                        String[] encabezadoValor = new String[2]; // Vector auxiliar para guardar los headers como llave-valor en el mapa.

                        for(int i=0; i < lineasEncabezados.length; i++) { // Se recorre vector con los encabezados.
                            encabezadoValor = lineasEncabezados[i].split(" "); // Separa los encabezados en llave-valor y lo guarda en el arreglo encabezado-valor.
                            mapaEncabezados.put(encabezadoValor[0], encabezadoValor[1]); // Guarda las posciones del arreglo en el mapa.
                            encabezadoValor = new String[2]; // Limpiamos el vector auxiliar.
                        }

                        // Accept, Content-type, Content-length, Date, Host, Referer, Server.
                        salida.println("HTTP/1.1 - 200 OK");
                        salida.println("Accept: " + mapaEncabezados.get("Accept:"));
                        salida.println("Content-type: text/html");
                        salida.println("Content-length: " + mapaEncabezados.get("Content-length:"));
                        salida.println("Date: " + new Date());
                        salida.println("Host: " + mapaEncabezados.get("Host:"));
                        salida.println("Referer: " + mapaEncabezados.get("Referer:"));
                        salida.println("Server: Servidor HTTP - TP1");
                        salida.println(""); // Linea en blanco entre encabezados y contenido. Esto es importante!

                        // Escribe a bitacora.
                        escribirBitacora(metodo, ""+mapaEncabezados.get("Host:"), ""+mapaEncabezados.get("Referer:"), archivoSolicitado, postData);

                        // Envía el HTML
                        salida.println("<H1>Bienvenido al Servidor: " + postData + "! </H1>");
                        salida.println("<H2>Los datos del usuario se obtuvieron mediante el metodo POST </H2>");
                        //salida.println("<H2>Request Method->" + request_method + "</H2>");
                        //salida.println("<H2>Post->" + postData + "</H2>");
                        salida.println("<form name=\"input\" action=\"form_submited\" method=\"post\">");
                        salida.println("Cambiar de usuario: <input type=\"text\" name=\"usuario\"><input type=\"submit\" value=\"Aceptar\"></form>");
                        salida.flush();
                    } catch(IOException e) {
                        e.printStackTrace();
                    }

                }

            }

        } catch(FileNotFoundException fnfe) {
            try {
                archivoNoEncontrado(salida, datosSalida, archivoSolicitado); // Si no se encuentra el archivo solicitado ejecuta este metodo, que muestra en pantalla la pagina de error 404.
            } catch(IOException ioe) {
                System.err.println("Error con la excepcion de achivo no encontrado: " + ioe.getMessage());
            }

        } catch(IOException ioe) {
            System.err.println("Error del servidor: " + ioe);
        } finally {
            try {
                entrada.close(); // Se cierra buffer de entrada.
                salida.close();  // Se cierra buffer de salida.
                datosSalida.close(); // Se cierra buffer de datos.
                conectar.close(); // Se cierra la conexion del socket.
            } catch(Exception e) {
                System.err.println("Error cerrandp el flujo: " + e.getMessage());
            }

            System.out.println("Conexion cerrada. \n");
        }
    }

    public byte[] leerDatosArchivo(File archivo, int larchivo) throws IOException {
        FileInputStream archEntrada = null; // Para almacenar archivo de entrada.
        byte[] datosArch = new byte[larchivo]; // Datos del archivo. larchivo es la longitud del archivo.

        try {
            archEntrada = new FileInputStream(archivo); // Crea un flujo de entrada desde archivo.
            archEntrada.read(datosArch); // Lee datosArch.length bytes desde el flujo que tiene archEntrada a el buffer o array datosArch.
        } finally {
            if(archEntrada != null) {
                archEntrada.close(); // Si archEntrada no era nulo, lo cierra.
            }
        }
        return datosArch; // Devuelve los datos del archivo en un arreglo de bytes.
    }

    public String getTipoContenido(String archSolicitado) {
        if(archSolicitado.endsWith(".htm") || archSolicitado.endsWith(".html")) {
            return "text/html";
        } else if(archSolicitado.endsWith(".jpg") || archSolicitado.endsWith(".jpeg")) {
            return "image/jpg";
        } else if(archSolicitado.endsWith(".gif")) {
            return "image/gif";
        } else if(archSolicitado.endsWith(".png")) {
            return "image/png";
        } else if(archSolicitado.endsWith(".mp3")) {
            return "audio/mpeg";
        } else {
            return "text/plain";
        }
    }

    public void archivoNoEncontrado(PrintWriter salida, OutputStream datosSalida, String archivoSolicitado) throws IOException {
        File archivoNE = new File(RAIZ_WEB, NO_ENCONTRADO);
        int longiArchivo = (int) archivoNE.length();
        String tMIMEConenido = "text/html"; // Tipo de media del archivo.
        byte[] datosArch = leerDatosArchivo(archivoNE, longiArchivo); // Lee el contenido para retornarselo al cliente.

        // Se mandan Encabezados o Headers HTTP con datos al cliente.
        salida.println("HTTP/1.1 - 404 Archivo no encontrado.");
        salida.println("Servidor (Server): Servidor HTTP - TP1");
        salida.println("Fecha (Date): " + new Date());
        salida.println("Tipo de contenido (Content-type): " + tMIMEConenido);
        salida.println("Longitud de contenido (Content-length): " + longiArchivo);
        salida.println("Acepta (Accept): text/html, text/plain"); // Tipos de media que acepta el servidor. Faltan imagenes.
        salida.println(); // Linea en blanco entre encabezados y contenido. Esto es importante!
        salida.flush(); // Se limpia el flujo de caracteres de salida.

        //Escribir datos a archivo.
        datosSalida.write(datosArch, 0, longiArchivo);
        datosSalida.flush();

        System.out.println("Archivo: " + archivoSolicitado + " no encontrado.");
    }

    public void escribirBitacora(String metodo, String servidor, String refiere, String url, String datos) {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        List<DatosBitacora> bitacora = new ArrayList<DatosBitacora>();
        String tiempo = Long.toString(timestamp.getTime());

        bitacora.add(new DatosBitacora(metodo, tiempo, servidor, refiere, url, datos));

        // Columnas de la bitacora.
        System.out.println("_________________________________________________________________________________________________________________________________________");
        System.out.printf("%10s %15s %20s %40s %30s %15s", "Metodo", "Estampilla", "Servidor", "Refiere", "Url", "Datos");
        System.out.println();

        // Datos de la bitacora.
        System.out.println("_________________________________________________________________________________________________________________________________________");
        datosBitacora += "_________________________________________________________________________________________________________________________________________" + "\n";
        for(DatosBitacora b: bitacora) {
            System.out.format("%10s %15s %20s %40s %30s %15s", b.getMetodo(), b.getEstampilla(), b.getServidor(), b.getRefiere(), b.getUrl(), b.getDatos());
            datosBitacora += String.format("%10s %15s %20s %40s %30s %15s", b.getMetodo(), b.getEstampilla(), b.getServidor(), b.getRefiere(), b.getUrl(), b.getDatos()) + "\n";
            System.out.println();
            //datosBitacora += "\n";
        }
        System.out.println("=======================================================================================================================================");
        datosBitacora += "=========================================================================================================================================" + "\n";

        escribirArchivo(datosBitacora);
    }

    public void escribirArchivo(String info) {
        try {
            FileWriter fileWriter = new FileWriter(archivoBitacora, true);
            fileWriter.write(info);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}