package model;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import model.Event.EventType;
import model.Patient.ColorCode;

public class Simulator {
	
	// Coda degli eventi
	private PriorityQueue<Event> queue;
	
	// Modello del mondo
	private List<Patient> patients;
	private PriorityQueue<Patient> expectant;
	private int freeStudios; // numero studi liberi
	
	private Patient.ColorCode lastColour;;
	
	// Parametri di input
	private int totStudios = 3; // NS
	
	private int numPatients = 120; // NP
	private Duration T_ARRIVAL = Duration.ofMinutes(5);
	
	private Duration DURATION_TRIAGE = Duration.ofMinutes(5);
	private Duration DURATION_WHITE = Duration.ofMinutes(10);
	private Duration DURATION_YELLOW = Duration.ofMinutes(15);
	private Duration DURATION_RED = Duration.ofMinutes(30);

	private Duration TIMEOUT_WHITE = Duration.ofMinutes(60);
	private Duration TIMEOUT_YELLOW = Duration.ofMinutes(30);
	private Duration TIMEOUT_RED = Duration.ofMinutes(30);
	
	private LocalTime startTime = LocalTime.of(8, 00);
	private LocalTime endTime = LocalTime.of(20, 00);
	
	// Parametri di output
	private int patientTreated;
	private int patientAbandoned;
	private int patientDead;
	
	// INIZIALIZZA IL SIMULATORE e crea gli eventi iniziali
	public void init() {
		// inizializza la coda di eventi
		this.queue = new PriorityQueue<>();
		
		// inizializza il modello del mondo
		this.patients = new ArrayList<>();
		this.expectant = new PriorityQueue<>();
		this.freeStudios = this.totStudios;
		
		// inizializza i parametri di output
		this.patientAbandoned = 0;
		this.patientDead = 0;
		this.patientTreated = 0;
		
		this.lastColour = ColorCode.RED;
		
		// inietta gli eventi di input (ARRIVAL)
		LocalTime ora = this.startTime;
		int inseriti = 0;
		
		this.queue.add(new Event(ora, EventType.TICK, null));
		
		while(ora.isBefore(endTime) && inseriti<this.numPatients) {
			Patient p = new Patient(ora, ColorCode.NEW, inseriti);
			this.patients.add(p);
			
			Event e = new Event(ora, EventType.ARRIVAL, p);
			this.queue.add(e);
			
			inseriti++;
			ora = ora.plus(T_ARRIVAL);
		}
	}
	
	private Patient.ColorCode nextColor() {
		if(lastColour.equals(ColorCode.WHITE))
			lastColour = ColorCode.YELLOW;
		else if(lastColour.equals(ColorCode.YELLOW))
			lastColour = ColorCode.RED;
		else
			lastColour = ColorCode.WHITE;
		
		return lastColour;
	}
	
	// ESEGUE LA SIMULAZIONE
	public void run() {
		while(!this.queue.isEmpty()) {
			Event e = this.queue.poll();
			System.out.println(e);
			processEvent(e);
		}
	}
	
	private void processEvent(Event e) {
		
		Patient p = e.getPatient();
		LocalTime ora = e.getTime();
		
		switch(e.getType()) {
		
		case ARRIVAL:
			this.queue.add(new Event(ora.plus(DURATION_TRIAGE), Event.EventType.TRIAGE, p));
			break;
			
		case TRIAGE:
			p.setColor(nextColor());
			if(p.getColor().equals(Patient.ColorCode.WHITE)) {
				this.queue.add(new Event(ora.plus(TIMEOUT_WHITE), EventType.TIMEOUT, p));
				this.expectant.add(p);
			}
			else if(p.getColor().equals(Patient.ColorCode.YELLOW)) {
				this.queue.add(new Event(ora.plus(TIMEOUT_YELLOW), EventType.TIMEOUT, p));
				this.expectant.add(p);
			}
			else if(p.getColor().equals(Patient.ColorCode.RED)) {
				this.queue.add(new Event(ora.plus(TIMEOUT_RED), EventType.TIMEOUT, p));
				this.expectant.add(p);}
			break;
			
		case FREE_STUDIO:
			if(this.freeStudios==0)
				break;
			// quale paziente ha diritto ad entrare??
			Patient first = this.expectant.poll();
			if(first!=null) {
				// ammetti il paziente nello studio
				if(first.getColor().equals(ColorCode.WHITE))
					this.queue.add(new Event(ora.plus(DURATION_WHITE), EventType.TREATED, first));
				else if(first.getColor().equals(ColorCode.YELLOW))
					this.queue.add(new Event(ora.plus(DURATION_YELLOW), EventType.TREATED, first));
				else if(first.getColor().equals(ColorCode.RED))
					this.queue.add(new Event(ora.plus(DURATION_RED), EventType.TREATED, first));
				first.setColor(ColorCode.TREATING);
				this.freeStudios--;
			}
			break;
			
		case TIMEOUT:
			Patient.ColorCode colour = p.getColor();
			switch(colour) {
			case WHITE:
				this.expectant.remove(p);
				p.setColor(ColorCode.OUT);
				this.patientAbandoned++;
				break;
				
			case YELLOW:
				this.expectant.remove(p);
				p.setColor(ColorCode.RED);
				this.queue.add(new Event(ora.plus(TIMEOUT_RED), EventType.TIMEOUT, p));
				this.expectant.add(p);
				break;
				
			case RED:
				this.expectant.remove(p);
				p.setColor(ColorCode.BLACK);
				this.patientDead++;
				break;
				
			default:
				//System.out.println("ERRORE: TIMEOUT CON COLORE "+colour);
			}
			break;
			
		case TREATED:
			this.patientTreated++;
			p.setColor(ColorCode.OUT);
			this.freeStudios++;
			this.queue.add(new Event(ora, EventType.FREE_STUDIO, null));
			break;
			
		case TICK:
			if(this.freeStudios>0 && !this.expectant.isEmpty())
				this.queue.add(new Event(ora, EventType.FREE_STUDIO, null));
			if(ora.isBefore(this.endTime))
				this.queue.add(new Event(ora.plus(Duration.ofMinutes(5)), EventType.TICK, null));
			break;
		}
		
	}

	public void setTotStudios(int totStudios) {
		this.totStudios = totStudios;
	}

	public void setNumPatients(int numPatients) {
		this.numPatients = numPatients;
	}

	public void setT_ARRIVAL(Duration t_ARRIVAL) {
		T_ARRIVAL = t_ARRIVAL;
	}

	public void setDURATION_TRIAGE(Duration dURATION_TRIAGE) {
		DURATION_TRIAGE = dURATION_TRIAGE;
	}

	public void setDURATION_WHITE(Duration dURATION_WHITE) {
		DURATION_WHITE = dURATION_WHITE;
	}

	public void setDURATION_YELLOW(Duration dURATION_YELLOW) {
		DURATION_YELLOW = dURATION_YELLOW;
	}

	public void setDURATION_RED(Duration dURATION_RED) {
		DURATION_RED = dURATION_RED;
	}

	public void setTIMEOUT_WHITE(Duration tIMEOUT_WHITE) {
		TIMEOUT_WHITE = tIMEOUT_WHITE;
	}

	public void setTIMEOUT_YELLOW(Duration tIMEOUT_YELLOW) {
		TIMEOUT_YELLOW = tIMEOUT_YELLOW;
	}

	public void setTIMEOUT_RED(Duration tIMEOUT_RED) {
		TIMEOUT_RED = tIMEOUT_RED;
	}

	public void setStartTime(LocalTime startTime) {
		this.startTime = startTime;
	}

	public void setEndTime(LocalTime endTime) {
		this.endTime = endTime;
	}
	
}
