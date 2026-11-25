package logica;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import modelo.ItemCarrito;
import modelo.Producto;
import modelo.Venta;

public class GestorTienda {

    // Atributos privados (Encapsulamiento - Unidad 3)
    private List<Producto> catalogo;
    private List<ItemCarrito> carrito;
    private List<Venta> historialVentas;
    private int contadorVentas;

    public GestorTienda() {
        this.catalogo = new ArrayList<>();
        this.carrito = new ArrayList<>();
        this.historialVentas = new ArrayList<>();
        this.contadorVentas = 1;
        cargarDatosIniciales();
    }

    private void cargarDatosIniciales() {
        registrarProducto("P001", "Taza Cerámica Corporativa", 12.00, 1500);
        registrarProducto("P002", "Lapicero Metálico Grabado", 4.50, 3000);
        registrarProducto("P003", "Cuaderno Ejecutivo A5", 18.00, 800);
        registrarProducto("P004", "Polo Publicitario Algodón", 25.00, 1200);
    }

    // --- GESTIÓN DEL CATÁLOGO ---
    public boolean registrarProducto(String codigo, String nombre, double precio, int stock) {
        // Condicionales (Unidad 1) y manejo de String (Unidad 3)
        if (codigo == null || codigo.trim().equals("") || precio <= 0 || stock < 0) {
            return false;
        }

        // Bucle FOR tradicional (Unidad 2) para evitar códigos duplicados
        for (int i = 0; i < catalogo.size(); i++) {
            Producto p = catalogo.get(i);
            if (p.getCodigo().equalsIgnoreCase(codigo.trim())) {
                return false;
            }
        }

        Producto nuevo = new Producto(codigo.trim(), nombre.trim(), precio, stock);
        catalogo.add(nuevo);
        return true;
    }

    public List<Producto> getCatalogo() {
        return catalogo;
    }

    // --- GESTIÓN DEL CARRITO ---
    public String agregarAlCarrito(Producto producto, int cantidad, boolean conEstampado) {
        if (producto == null) {
            return "Seleccione un producto válido.";
        }
        if (cantidad <= 0) {
            return "La cantidad debe ser mayor a 0.";
        }

        // Calcular cuánto de este producto ya está en el carrito
        int cantidadEnCarrito = 0;
        for (int i = 0; i < carrito.size(); i++) {
            ItemCarrito item = carrito.get(i);
            if (item.getProducto().getCodigo().equals(producto.getCodigo())) {
                cantidadEnCarrito = cantidadEnCarrito + item.getCantidad();
            }
        }

        // Validar stock disponible
        if (cantidad + cantidadEnCarrito > producto.getStock()) {
            int disponible = producto.getStock() - cantidadEnCarrito;
            return "Stock insuficiente. Disponible: " + disponible;
        }

        // Si ya existe en el carrito con la misma opción de estampado, sumar cantidad
        for (int i = 0; i < carrito.size(); i++) {
            ItemCarrito item = carrito.get(i);
            if (item.getProducto().getCodigo().equals(producto.getCodigo()) 
                    && item.isConEstampado() == conEstampado) {
                item.setCantidad(item.getCantidad() + cantidad);
                return "OK";
            }
        }

        ItemCarrito nuevoItem = new ItemCarrito(producto, cantidad, conEstampado);
        carrito.add(nuevoItem);
        return "OK";
    }

    public void eliminarItemCarrito(int indice) {
        if (indice >= 0 && indice < carrito.size()) {
            carrito.remove(indice);
        }
    }

    public List<ItemCarrito> getCarrito() {
        return carrito;
    }

    // --- CÁLCULOS FINANCIEROS (Unidades 1 y 2) ---
    public int calcularTotalUnidades() {
        int total = 0;
        for (int i = 0; i < carrito.size(); i++) {
            total = total + carrito.get(i).getCantidad();
        }
        return total;
    }

    public double calcularSubtotal() {
        double subtotal = 0.0;
        for (int i = 0; i < carrito.size(); i++) {
            subtotal = subtotal + carrito.get(i).getSubtotal();
        }
        return subtotal;
    }

    // Descuento del 5% si totalUnidades > 1000
    public double calcularDescuento() {
        int unidades = calcularTotalUnidades();
        if (unidades > 1000) {
            return calcularSubtotal() * 0.05;
        } else {
            return 0.0;
        }
    }

    // IGV 18% sobre (subtotal - descuento)
    public double calcularIGV() {
        double baseImponible = calcularSubtotal() - calcularDescuento();
        return baseImponible * 0.18;
    }

    // Total Final = subtotal - descuento + IGV
    public double calcularTotalFinal() {
        return (calcularSubtotal() - calcularDescuento()) + calcularIGV();
    }

    // --- CONFIRMAR VENTA Y EXPORTAR BOLETA TXT ---
    public Venta confirmarVenta(String cliente) {
        if (carrito.size() == 0) {
            return null;
        }
        if (cliente == null || cliente.trim().equals("")) {
            cliente = "Cliente General";
        }

        // Generar código correlativo con String e IF (Ej: V-001, V-010, V-100)
        String idVenta = "";
        if (contadorVentas < 10) {
            idVenta = "V-00" + contadorVentas;
        } else if (contadorVentas < 100) {
            idVenta = "V-0" + contadorVentas;
        } else {
            idVenta = "V-" + contadorVentas;
        }
        contadorVentas++;

        // Fecha estándar usando java.util.Date (sin librerías complejas)
        String fecha = new java.util.Date().toString();

        int totalUnidades = calcularTotalUnidades();
        double subtotal = calcularSubtotal();
        double descuento = calcularDescuento();
        double igv = calcularIGV();
        double totalPagado = calcularTotalFinal();

        // 1. Descontar stock de cada producto
        for (int i = 0; i < carrito.size(); i++) {
            ItemCarrito item = carrito.get(i);
            Producto p = item.getProducto();
            p.setStock(p.getStock() - item.getCantidad());
        }

        // 2. Exportar boleta en archivo .txt
        exportarBoletaTxt(idVenta, fecha, cliente.trim(), totalUnidades, subtotal, descuento, igv, totalPagado);

        // 3. Guardar en historialVentas y limpiar carrito
        Venta nuevaVenta = new Venta(idVenta, fecha, cliente.trim(), totalUnidades, totalPagado);
        historialVentas.add(nuevaVenta);
        carrito.clear();

        return nuevaVenta;
    }

    // Manejo de excepciones con Try / Catch / Finally (Unidad 3)
    private void exportarBoletaTxt(String idVenta, String fecha, String cliente, int unidades,
                                   double subtotal, double descuento, double igv, double total) {
        String nombreArchivo = "Boleta_" + idVenta + ".txt";
        FileWriter fw = null;
        PrintWriter pw = null;

        try {
            fw = new FileWriter(nombreArchivo);
            pw = new PrintWriter(fw);

            pw.println("==============================================");
            pw.println("        TIENDA DE MERCHANDISING S.A.C.        ");
            pw.println("               BOLETA DE VENTA                ");
            pw.println("==============================================");
            pw.println("N° Boleta : " + idVenta);
            pw.println("Fecha     : " + fecha);
            pw.println("Cliente   : " + cliente);
            pw.println("----------------------------------------------");
            pw.println("DETALLE DE PRODUCTOS:");

            for (int i = 0; i < carrito.size(); i++) {
                ItemCarrito item = carrito.get(i);
                String extra = "";
                if (item.isConEstampado()) {
                    extra = " (+Logo)";
                }
                pw.println("- " + item.getProducto().getNombre() + extra
                        + " | Cant: " + item.getCantidad()
                        + " | P.Unit: S/ " + Math.round(item.getPrecioFinalUnitario() * 100.0) / 100.0
                        + " | Sub: S/ " + Math.round(item.getSubtotal() * 100.0) / 100.0);
            }

            pw.println("----------------------------------------------");
            pw.println("Total Unidades : " + unidades);
            pw.println("Subtotal       : S/ " + Math.round(subtotal * 100.0) / 100.0);
            pw.println("Descuento (5%) : -S/ " + Math.round(descuento * 100.0) / 100.0);
            pw.println("IGV (18%)      : S/ " + Math.round(igv * 100.0) / 100.0);
            pw.println("TOTAL A PAGAR  : S/ " + Math.round(total * 100.0) / 100.0);
            pw.println("==============================================");
            pw.println("        ¡Gracias por su preferencia!          ");

        } catch (IOException e) {
            System.out.println("Error al generar la boleta: " + e.getMessage());
        } finally {
            try {
                if (pw != null) {
                    pw.close();
                }
                if (fw != null) {
                    fw.close();
                }
            } catch (IOException ex) {
                System.out.println("Error al cerrar el archivo: " + ex.getMessage());
            }
        }
    }

    // --- HISTORIAL ---
    public List<Venta> getHistorialVentas() {
        return historialVentas;
    }

    public double calcularTotalRecaudado() {
        double total = 0.0;
        for (int i = 0; i < historialVentas.size(); i++) {
            total = total + historialVentas.get(i).getTotalPagado();
        }
        return total;
    }
}