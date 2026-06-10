package com.psicosocial.simulador.service;

import com.psicosocial.simulador.dto.HelpSectionDto;

import java.util.List;

public final class HelpService {

    private HelpService() {}

    public static List<HelpSectionDto> getSections() {
        return List.of(
                HelpSectionDto.builder()
                        .id("simulator")
                        .title("Simulador")
                        .icon("sports_esports")
                        .items(List.of(
                                item("¿Cómo inicio un caso?",
                                        "Desde el Dashboard, selecciona un caso y pulsa Iniciar Caso. Explora la escena con WASD o los controles táctiles y responde cada decisión."),
                                item("¿Qué significan las categorías de puntaje?",
                                        "Cada pregunta evalúa una dimensión: Clínica (intervención técnica), Ética (principios profesionales) y Normativa (rutas legales e institucionales)."),
                                item("¿Cuántos intentos tengo?",
                                        "Por defecto 3 intentos por caso. Si los agotas sin aprobar, debes solicitar reinicio al profesor."),
                                item("¿Cuál es el puntaje mínimo para aprobar?",
                                        "Debes obtener al menos 60% en el promedio de las tres dimensiones.")
                        ))
                        .build(),
                HelpSectionDto.builder()
                        .id("access")
                        .title("Acceso y cuenta")
                        .icon("key")
                        .items(List.of(
                                item("¿Por qué me piden un código de acceso?",
                                        "Los estudiantes deben ser autorizados por el profesor. Tras iniciar sesión, el profesor aprueba tu solicitud y recibes un código de 6 dígitos."),
                                item("¿Puedo iniciar sesión con Google?",
                                        "Sí, si el administrador configuró OAuth. El flujo de código de acceso aplica igual para estudiantes."),
                                item("¿Cómo recupero mi contraseña?",
                                        "En la pantalla de login, usa Olvidé mi contraseña. Recibirás un código por correo si SMTP está configurado.")
                        ))
                        .build(),
                HelpSectionDto.builder()
                        .id("professor")
                        .title("Para profesores")
                        .icon("school")
                        .items(List.of(
                                item("¿Cómo creo casos con preguntas?",
                                        "Ve a Gestión de Casos, crea el caso y usa el botón Preguntas para agregar decisiones con opciones, categoría y feedback."),
                                item("¿Cómo asigno casos a un grupo?",
                                        "En Grupos, edita un grupo y selecciona los casos asignados. Solo esos estudiantes verán esos casos en su dashboard."),
                                item("¿Cómo apruebo reinicios?",
                                        "Las solicitudes aparecen en Reinicios y en las notificaciones. Al aprobar, el estudiante recupera sus intentos.")
                        ))
                        .build(),
                HelpSectionDto.builder()
                        .id("legal")
                        .title("Privacidad y términos")
                        .icon("gavel")
                        .items(List.of(
                                item("Privacidad de datos",
                                        "Los datos académicos (intentos, puntajes, respuestas) se almacenan para fines educativos. No se comparten con terceros."),
                                item("Uso académico",
                                        "Esta plataforma está diseñada para formación en psicología social. Las decisiones del simulador no sustituyen protocolos institucionales reales."),
                                item("Contacto",
                                        "Para soporte técnico, contacta al administrador de tu institución o al profesor del curso.")
                        ))
                        .build()
        );
    }

    private static HelpSectionDto.HelpItemDto item(String question, String answer) {
        return HelpSectionDto.HelpItemDto.builder().question(question).answer(answer).build();
    }
}
