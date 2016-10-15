import processing.serial.*;

// TURN DEBUG_MODE false BEFORE USING

Serial port = null;
Dispatch dis; // Utility class
boolean first = true; // First point on canvas
ArrayList<PVector> p; // Position vectors of all mouse clicks
ArrayList<PVector> d; // Direction vectors

void setup() {
  size(800, 600);
  if(!DEBUG_MODE)
    port = new Serial(this, "COM3", 9600); 
  background(255);
  d = new ArrayList<PVector>();
  p = new ArrayList<PVector>();
  dis = new Dispatch(port);
}

void draw(){
  background(255);
  if(first) return;
  for(int i = 0; i < p.size() - 1; i++){
    line(p.get(i).x,p.get(i).y,p.get(i+1).x,p.get(i+1).y);
  }
  int i = p.size() - 1;
  line(p.get(i).x,p.get(i).y,mouseX,mouseY);
}

void keyPressed() {
  // The movement keys are for debugging
  switch(key){
  case 'w': 
    if(!DEBUG_MODE)
      port.write(F);
  break;
  case 's':
    if(!DEBUG_MODE)
      port.write(B);
  break;
  case 'a': 
    if(!DEBUG_MODE)
      port.write(L); 
  break;
  case 'd': 
    if(!DEBUG_MODE)
      port.write(R);  
  break;
  case 'q':
    if(!DEBUG_MODE)
      port.write(H);
  break;  
  case 'e':
    dis.dispatch(d);
  break;
  }
}


void mousePressed(){
  p.add(new PVector(mouseX,mouseY));  
  if(first){
    first = false;
    return;
  }
  int i = p.size() - 1;
  PVector temp = PVector.sub(p.get(i),p.get(i-1));
  d.add(temp);
}
