package me.ichun.mods.ichunutil.client.gui.bns.window.view.element;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import me.ichun.mods.ichunutil.client.gui.bns.Theme;
import me.ichun.mods.ichunutil.client.gui.bns.window.Fragment;
import net.minecraft.client.resources.language.I18n;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

public class ElementToggle<T extends ElementToggle> extends ElementClickable<T>
{
    public String text;
    public boolean toggleState;

    public ElementToggle(@Nonnull Fragment parent, @Nonnull String s, Consumer<T> callback)
    {
        super(parent, callback);
        text = !s.isEmpty() ? I18n.get(s) : "";
    }

    public <T extends ElementToggle<?>> T setToggled(boolean flag)
    {
        toggleState = flag;
        return (T)this;
    }

    @Override
    public void render(PoseStack stack, int mouseX, int mouseY, float partialTick)
    {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        super.render(stack, mouseX, mouseY, partialTick);
        if(renderMinecraftStyle() > 0)
        {
            renderMinecraftStyleButton(stack, getLeft(), getTop(), width, height, disabled || parentFragment.isDragging() && parentFragment.getFocused() == this || toggleState ? ButtonState.CLICK : hover ? ButtonState.HOVER : ButtonState.IDLE, renderMinecraftStyle());
        }
        else
        {
            fill(stack, getTheme().elementButtonBorder, 0);
            int[] colour;
            if(disabled)
            {
                colour = getTheme().elementButtonBackgroundInactive;
            }
            else if(parentFragment.isDragging() && parentFragment.getFocused() == this)
            {
                colour = getTheme().elementButtonClick;
            }
            else if(toggleState && hover)
            {
                colour = getTheme().elementButtonToggleHover;
            }
            else if(hover)
            {
                colour = getTheme().elementButtonBackgroundHover;
            }
            else if(toggleState)
            {
                colour = getTheme().elementButtonToggle;
            }
            else
            {
                colour = getTheme().elementButtonBackgroundInactive;
            }
            fill(stack, colour, 1);
        }
        renderText(stack);
    }

    public void renderText(PoseStack stack)
    {
        if(!text.isEmpty())
        {
            String s = reString(text, width - 4);

            //draw the text
            if(renderMinecraftStyle() > 0)
            {
                getFontRenderer().drawShadow(stack, s, getLeft() + (this.width - getFontRenderer().width(s)) / 2F, getTop() + (height - getFontRenderer().lineHeight) / 2F + 1, getMinecraftFontColour());
            }
            else
            {
                getFontRenderer().draw(stack, s, getLeft() + (this.width - getFontRenderer().width(s)) / 2F, getTop() + (height - getFontRenderer().lineHeight) / 2F + 1, Theme.getAsHex(toggleState ? getTheme().font : getTheme().fontDim));
            }
        }
    }

    @Nullable
    @Override
    public String tooltip(double mouseX, double mouseY)
    {
        if(!text.isEmpty())
        {
            String s = reString(text, width - 4);
            if(!s.equals(text))
            {
                String tooltip = super.tooltip(mouseX, mouseY);
                if(tooltip != null)
                {
                    return text + " - " + tooltip;
                }
                return text;
            }
        }
        return super.tooltip(mouseX, mouseY);
    }

    @Override
    public void onClickRelease()
    {
        toggleState = !toggleState;
    }

    @Override
    public int getMinWidth()
    {
        return 14;
    }

    @Override
    public int getMinHeight()
    {
        return 14;
    }
}
