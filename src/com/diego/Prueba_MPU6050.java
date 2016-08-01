package com.diego;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Prueba_MPU6050 {
    
    public static void main(String[] args) {
        try {
            MPU6050 mpu6050 = new MPU6050();
        } catch (IOException ex) {
            System.err.println("No se pudo crear el objeto");
        }        
    }    
}
