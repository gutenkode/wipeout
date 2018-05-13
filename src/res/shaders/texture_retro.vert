// texture vertex shader
#version 330 core

layout(location = 0) in vec4 VertexIn;
//layout(location = 1) in vec4 ColorIn;
layout(location = 2) in vec2 TexIn;

//out vec4 color;
noperspective out vec2 texCoord;

uniform mat4 projectionMatrix = mat4(1.0);
uniform mat4 viewMatrix  = mat4(1.0);
uniform mat4 modelMatrix  = mat4(1.0);

void main()
{
	gl_Position = projectionMatrix * viewMatrix * modelMatrix * VertexIn;
	gl_Position.xyz = floor(gl_Position.xyz * 30) /30;

	//color = ColorIn;
	texCoord = TexIn;
}
