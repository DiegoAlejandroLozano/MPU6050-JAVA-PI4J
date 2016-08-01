
package com.diego;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class MPU6050 {
    
    //Registros de configuración-------------------------------        
        public static final byte SMPLRT_DIV     = 25;
        public static final byte CONFIG         = 26;
        public static final byte ACCEL_CONFIG   = 28;
        public static final byte FIFO_EN        = 35;
        public static final byte INT_ENABLE     = 56;  
        public static final byte PWR_MGMT_1     = 107;
        public static final byte PWR_MGMT_2     = 108;
    //fin de los registro de configuración---------------------
        
    //Resoluciones acelerómetro--------------------------------
        public static final double resolAc0  = 0.000061035; 
        public static final double resolAc1  = 0.00012207;
        public static final double resolAc2  = 0.00024414;
        public static final double resolAc3  = 0.000488281;
    //Fin de las resoluciones acelerómetro---------------------

    //Variables de clase para el bus I2C
        private I2CBus bus;
        private I2CDevice acelerometro;
    //Fin de las variables para el bus I2C 
    
    //Variables para almacenar las componentes del acelerómetro.................
        private double acX = 0.0, acY = 0.0, acZ = 0.0;
    //Fin de las variables...................................................... 
        
    //Constructor de la clase    
    public MPU6050() throws IOException {  
        
        //Inicializando la comunicación I2C********************
            bus = I2CFactory.getInstance( I2CBus.BUS_1 );
            acelerometro = bus.getDevice( 0x69 ); 
        //Fin de la inicialización de la comunicación I2C******
            
        //Inicializando el acelerometro*****************************************
            
            acelerometro.write( SMPLRT_DIV,     (byte) 0b00000000 );
            acelerometro.write( CONFIG,         (byte) 0b00000000 );
            
            acelerometro.write( ACCEL_CONFIG,   (byte) 0b00000000 );
            acelerometro.write( ACCEL_CONFIG,   (byte) 0b00001000 );
            acelerometro.write( ACCEL_CONFIG,   (byte) 0b00010000 );
            acelerometro.write( ACCEL_CONFIG,   (byte) 0b00011000 );
            
            acelerometro.write( FIFO_EN,        (byte) 0b00000000 );
            acelerometro.write( INT_ENABLE,     (byte) 0b00000000 );
            acelerometro.write( PWR_MGMT_1,     (byte) 0b00000001 );
            acelerometro.write( PWR_MGMT_2,     (byte) 0b00000000 );
            
        //Fin de la configuración del acelerómetro******************************
            
        comenzarLectura();
    }//Fin del constructor   
    
    //Método para realizar la lectura del MPU6050
    public void comenzarLectura(){
              
        //Arrays para almacenar los valores de las componentes X, Y, Z del acelerometro
        //y del giroscopio
            byte[] accelData = new byte[6];
        //Fin de la declaración de los arrays ............................................. 
            
        //Tiempo de retardo en el ciclo While**
            final int tiempo = 1;
        //Fin del tiempo de retardo************
            
        //Tiempo de muestreo para el filtro pasa bajas y pasa altas***
            final double t = tiempo / 1000.0;
        //Fin del tiempo de muestreo**********************************
            
            
        //Variables para el filtro pasa bajas del acelerómetro******************
            double acX_anterior = 0.0, acY_anterior = 0.0, acZ_anterior = 0.0;
            double acX_filtrado = 0.0, acY_filtrado = 0.0, acZ_filtrado = 0.0;
            
            /*Wo1 es la frecuencia de corte
            ao1 es la ganancia del filtro*/
            final double ao1 = 1, Wo1 = 5;  
                         
            //Definción de las constantes para filtro pasa bajas
                final double a = ( ao1*t ) / ( 2 + Wo1*t );
                final double b = t / ( 2 + Wo1*t );
                final double c = ( Wo1*t - 2 ) / ( 2 + Wo1*t );
            //Fin de la definción de las constantes*************
        //Fin de las variables para el filtro pasa bajas************************
                                
        int i = 0;
        while( i < 10000 ){
            try {
                
                //Lee el acelerometro******************************************
                    acelerometro.read(0x3B, accelData, 0, 6);                    
                    acX = (resolAc0 * make16( accelData[0],  accelData[1] ));
                    acY = (resolAc0 * make16( accelData[2] , accelData[3] ));
                    acZ = (resolAc0 * make16( accelData[4] , accelData[5] ));                       
                //Fin de la lecutra********************************************  
                                             
                //Ecuación en diferencias del filtro pasa bajas*****************
                    acX_filtrado = a*acX + b*acX_anterior - c*acX_filtrado;
                    acX_anterior = acX;
                    
                    acY_filtrado = a*acY + b*acY_anterior - c*acY_filtrado;
                    acY_anterior = acY;
                    
                    acZ_filtrado = a*acZ + b*acZ_anterior - c*acZ_filtrado;
                    acZ_anterior = acZ;
                //Fin de la ecuación en diferencias del filtro pasa bajas*******    
                    
                //Cálculo de alpha y theta filtrado********************************************************
                    double w1 = Math.sqrt( Math.pow( acZ_filtrado , 2) + Math.pow( acY_filtrado , 2) );
                    double v1 = Math.sqrt( Math.pow( acX_filtrado , 2) + Math.pow( acY_filtrado , 2) );
                
                    double theta_filtrado = Math.toDegrees( Math.atan( acX_filtrado / w1 ) );                               
                    double alpha_filtrado = Math.toDegrees( Math.atan( acZ_filtrado / v1 ) );
                    System.out.printf("Theta = %4.0f\t Alpha = %4.0f\n", theta_filtrado, alpha_filtrado);
                //Fin del cálculo de alpha y theta filtrado************************************************              
                    
            Thread.sleep( tiempo );
                    
            } catch (IOException ex) {
                Logger.getLogger(MPU6050.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InterruptedException ex) {
                Logger.getLogger(MPU6050.class.getName()).log(Level.SEVERE, null, ex);
            } finally{
                i++;
            }       
                
        }//Fin del ciclo while
    } //Fin del método para leer el acelerómetro  
    
    private int make16( int high, int low ){
        //Este método convierte dos variables de 8 bits, en una sola variable de de 16 bits
        return ( (high << 8) | low );
    }
}
