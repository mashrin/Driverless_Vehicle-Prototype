import processing.serial.*;
// Character codes to interface with Arduino
final char L = '1';
final char R = '2';
final char F = '3';
final char B = '4';
final char H = '5';

final boolean DEBUG_MODE = false;

class Dispatch{  
  private final float ST = 1720.6498f; // Millies
  private final float MSCM = 134.5895f; // Millies
  private final float CMPX = .1f;
  private PVector h;
  private Serial port;
  
  public Dispatch(Serial port){
    h = new PVector(0,-1);
    this.port = port;
  }
  public void dispatch(ArrayList<PVector> d){
    // Target time in millies
    long target;
    
    // Iterate through all the direction vectors
    for(PVector dir:d){
      // Find angle between heading and direction
      float theta = PVector.angleBetween(dir,h);
      float dirMag = dir.mag();
      dir.normalize();
      float delta = h.x*dir.y - h.y * dir.x;
      if(delta < 0){
        if(!DEBUG_MODE)
          port.write(L);
        print("Left: ",degrees(theta),"\n");        
      } else if (delta > 0) {
        if(!DEBUG_MODE)
          port.write(R);
        print("Right: ",degrees(theta),"\n");  
      }     
      target = millis() + (long)(ST * theta);
      while(millis()<target);
      if(!DEBUG_MODE)
        port.write(F); 
      target = (long)(dirMag * MSCM * CMPX) + millis();      
      print("Forward: ",(dirMag * MSCM * CMPX),"\n"); 
      while(millis()<target);   
      h = new PVector(dir.x,dir.y);  
    }
    if(!DEBUG_MODE)
      port.write(H);
    print("Done\n");
  }
}
