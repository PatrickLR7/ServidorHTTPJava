public class DatosBitacora {
    private String metodo;
    private String estampilla;
    private String servidor;
    private String refiere;
    private String url;
    private String datos;

    // Constructor con parametros
    public DatosBitacora(String elMetodo, String laEstampilla, String elServidor, String elReferente, String elUrl, String losDatos) {
        metodo = elMetodo;
        estampilla = laEstampilla;
        servidor = elServidor;
        refiere = elReferente;
        url = elUrl;
        datos = losDatos;
    }

    // Metodos Get
    public String getMetodo() {
        return metodo;
    }
    public String getEstampilla() {
        return estampilla;
    }
    public String getServidor() {
        return servidor;
    }
    public String getRefiere() {
        return refiere;
    }
    public String getUrl() {
        return url;
    }
    public String getDatos() {
        return datos;
    }

    //Metodos Set
    public void setMetodo(String elMetodo){
        metodo = elMetodo;
    }
    public void setEstampilla(String laEstampilla){
        estampilla = laEstampilla;
    }
    public void setServidor(String elServidor){
        servidor = elServidor;
    }
    public void setRefiere(String elReferente){
        refiere = elReferente;
    }
    public void setUrl(String elUrl){
        url = elUrl;
    }
    public void setDatos(String losDatos){
        datos = losDatos;
    }
}